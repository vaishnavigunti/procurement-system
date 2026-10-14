package com.infosys.procurement_system.requisition;

import com.infosys.procurement_system.approval.Approval;
import com.infosys.procurement_system.approval.ApprovalRepository;
import com.infosys.procurement_system.approval.ApprovalResponse;
import com.infosys.procurement_system.approval.ApprovalStatus;
import com.infosys.procurement_system.approvalworkflow.ApprovalWorkflow;
import com.infosys.procurement_system.approvalworkflow.ApprovalWorkflowRepository;
import com.infosys.procurement_system.approvalworkflow.ApprovalWorkflowStep;
import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.category.Category;
import com.infosys.procurement_system.category.CategoryRepository;
import com.infosys.procurement_system.common.CurrentUserProvider;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.BadRequestException;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ForbiddenOperationException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.user.User;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Purchase requisition lifecycle: draft, edit, submit (picks the approval chain), cancel. Decisions live in ApprovalService. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequisitionService {

    private static final String TYPE = "REQUISITION";
    private static final Set<String> SEES_ALL = Set.of("ADMIN", "FINANCE_HEAD", "PROCUREMENT_OFFICER");
    private static final Set<String> SEES_DEPARTMENT = Set.of("MANAGER", "DEPARTMENT_HEAD");
    private static final Set<RequisitionStatus> COMMITTED = EnumSet.of(
            RequisitionStatus.PENDING_APPROVAL, RequisitionStatus.APPROVED, RequisitionStatus.CONVERTED);

    private final RequisitionRepository requisitionRepository;
    private final RequisitionItemRepository itemRepository;
    private final CategoryRepository categoryRepository;
    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    public PageResponse<RequisitionResponse> list(RequisitionStatus status, Boolean mine, Pageable pageable) {
        User me = currentUserProvider.requireCurrentUser();
        String role = me.getRole().getName();
        boolean onlyMine = Boolean.TRUE.equals(mine);

        Specification<Requisition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate isMine = cb.equal(root.get("requester").get("id"), me.getId());
            if (onlyMine) {
                predicates.add(isMine);
            } else if (SEES_DEPARTMENT.contains(role)) {
                predicates.add(cb.or(isMine, cb.equal(root.get("department").get("id"), me.getDepartment().getId())));
            } else if (!SEES_ALL.contains(role)) {
                predicates.add(isMine);
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(requisitionRepository.findAll(spec, pageable), RequisitionResponse::from);
    }

    public RequisitionResponse get(Long id) {
        Requisition r = find(id);
        assertCanView(currentUserProvider.requireCurrentUser(), r);
        return RequisitionResponse.from(r);
    }

    public List<ApprovalResponse> approvalHistory(Long id) {
        Requisition r = find(id);
        assertCanView(currentUserProvider.requireCurrentUser(), r);
        return approvalRepository.findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(id)
                .stream().map(ApprovalResponse::from).toList();
    }

    @Transactional
    public RequisitionResponse create(RequisitionRequest request) {
        User me = currentUserProvider.requireCurrentUser();
        List<RequisitionItem> items = buildItems(request.items());

        Requisition r = new Requisition();
        r.setRequisitionNumber(String.format("PR-%d-%05d", Year.now().getValue(),
                requisitionRepository.nextRequisitionNumber()));
        r.setRequester(me);
        r.setDepartment(me.getDepartment());
        applyHeader(r, request);
        r.setTotalAmount(total(items));
        Requisition saved = requisitionRepository.save(r);
        attachItems(saved, items);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED,
                "Created " + saved.getRequisitionNumber() + " total " + saved.getTotalAmount());
        return RequisitionResponse.from(saved);
    }

    @Transactional
    public RequisitionResponse update(Long id, RequisitionRequest request) {
        User me = currentUserProvider.requireCurrentUser();
        Requisition r = find(id);
        assertOwner(me, r);
        if (r.getStatus() != RequisitionStatus.DRAFT && r.getStatus() != RequisitionStatus.RETURNED) {
            throw new ConflictException("Only DRAFT or RETURNED requisitions can be edited.");
        }
        List<RequisitionItem> items = buildItems(request.items());
        itemRepository.deleteAll(itemRepository.findByRequisitionId(id));
        itemRepository.flush();
        r.getItems().clear();
        applyHeader(r, request);
        r.setTotalAmount(total(items));
        attachItems(r, items);
        auditService.record(TYPE, id, AuditAction.UPDATED,
                "Updated " + r.getRequisitionNumber() + " total " + r.getTotalAmount());
        return RequisitionResponse.from(r);
    }

    @Transactional
    public RequisitionResponse submit(Long id, RequisitionSubmitRequest request) {
        User me = currentUserProvider.requireCurrentUser();
        Requisition r = find(id);
        assertOwner(me, r);
        if (r.getStatus() != RequisitionStatus.DRAFT && r.getStatus() != RequisitionStatus.RETURNED) {
            throw new ConflictException("Only DRAFT or RETURNED requisitions can be submitted.");
        }
        if (r.getItems().isEmpty()) {
            throw new BadRequestException("Add at least one item before submitting.");
        }
        if (r.getTotalAmount().signum() <= 0) {
            throw new BadRequestException("The total amount must be greater than zero.");
        }

        ApprovalWorkflow workflow = workflowRepository.findMatchingWorkflow(r.getTotalAmount());
        if (workflow == null) {
            throw new ConflictException("No active approval workflow covers amount " + r.getTotalAmount() + ".");
        }
        List<ApprovalWorkflowStep> steps = workflow.getSteps();
        if (steps.isEmpty()) {
            throw new ConflictException("Workflow '" + workflow.getName() + "' has no approval steps.");
        }

        checkDepartmentBudget(r);

        int round = nextRound(id);
        for (ApprovalWorkflowStep step : steps) {
            Approval approval = new Approval();
            approval.setRequisition(r);
            approval.setWorkflowStep(step);
            approval.setRoundNumber(round);
            approval.setStatus(ApprovalStatus.PENDING);
            approvalRepository.save(approval);
        }
        r.setJustification(request.justification().trim());
        r.setWorkflow(workflow);
        r.setStatus(RequisitionStatus.PENDING_APPROVAL);
        r.setCurrentStep(steps.get(0).getStepOrder());
        r.setSubmittedAt(Instant.now());
        auditService.record(TYPE, id, AuditAction.SUBMITTED,
                "Submitted " + r.getRequisitionNumber() + " to workflow '" + workflow.getName()
                        + "' (round " + round + ", " + steps.size() + " steps)");
        return RequisitionResponse.from(r);
    }

    @Transactional
    public RequisitionResponse cancel(Long id, RequisitionCancelRequest request) {
        User me = currentUserProvider.requireCurrentUser();
        Requisition r = find(id);
        boolean isAdmin = "ADMIN".equals(me.getRole().getName());
        if (!isAdmin) {
            assertOwner(me, r);
        }
        if (r.getStatus() != RequisitionStatus.DRAFT && r.getStatus() != RequisitionStatus.PENDING_APPROVAL
                && r.getStatus() != RequisitionStatus.RETURNED) {
            throw new ConflictException("A " + r.getStatus() + " requisition cannot be cancelled.");
        }
        for (Approval a : approvalRepository.findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(id)) {
            if (a.getStatus() == ApprovalStatus.PENDING) {
                a.setStatus(ApprovalStatus.CANCELLED);
            }
        }
        r.setStatus(RequisitionStatus.CANCELLED);
        r.setCancelReason(request.reason().trim());
        auditService.record(TYPE, id, AuditAction.CANCELLED,
                "Cancelled " + r.getRequisitionNumber() + ": " + r.getCancelReason());
        return RequisitionResponse.from(r);
    }

    // ---------------------------------------------------------------- helpers

    private Requisition find(Long id) {
        return requisitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Requisition", id));
    }

    private void assertOwner(User me, Requisition r) {
        if (!r.getRequester().getId().equals(me.getId())) {
            throw new ForbiddenOperationException("Only the requester can do this.");
        }
    }

    private void assertCanView(User me, Requisition r) {
        String role = me.getRole().getName();
        boolean allowed = r.getRequester().getId().equals(me.getId())
                || SEES_ALL.contains(role)
                || (SEES_DEPARTMENT.contains(role)
                        && r.getDepartment().getId().equals(me.getDepartment().getId()));
        if (!allowed) {
            throw new ForbiddenOperationException("You are not allowed to view this requisition.");
        }
    }

    private void applyHeader(Requisition r, RequisitionRequest request) {
        r.setTitle(request.title().trim());
        r.setJustification(request.justification());
        r.setPriority(request.priority());
        r.setRequiredByDate(request.requiredByDate());
        if (request.requiredByDate() != null && request.requiredByDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Required-by date cannot be in the past.");
        }
    }

    /** Builds item entities (not yet linked to a requisition) and validates categories. */
    private List<RequisitionItem> buildItems(List<RequisitionItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("At least one item is required.");
        }
        List<RequisitionItem> items = new ArrayList<>();
        for (RequisitionItemRequest req : requests) {
            Category category = categoryRepository.findById(req.categoryId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Category", req.categoryId()));
            if (!category.isActive()) {
                throw new ConflictException("Category " + category.getName() + " is inactive.");
            }
            RequisitionItem item = new RequisitionItem();
            item.setCategory(category);
            item.setItemName(req.itemName().trim());
            item.setDescription(req.description());
            item.setQuantity(req.quantity());
            item.setUnit(req.unit().trim().toUpperCase());
            item.setEstimatedUnitPrice(req.estimatedUnitPrice());
            items.add(item);
        }
        return items;
    }

    private void attachItems(Requisition r, List<RequisitionItem> items) {
        for (RequisitionItem item : items) {
            item.setRequisition(r);
            r.getItems().add(itemRepository.save(item));
        }
    }

    private BigDecimal total(List<RequisitionItem> items) {
        return items.stream()
                .map(i -> i.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int nextRound(Long requisitionId) {
        List<Approval> all = approvalRepository
                .findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(requisitionId);
        return all.isEmpty() ? 1 : all.get(all.size() - 1).getRoundNumber() + 1;
    }

    /** Spending control: committed spend this year plus this requisition must fit the department budget. */
    private void checkDepartmentBudget(Requisition r) {
        Instant yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        BigDecimal committed = requisitionRepository.sumCommittedAmount(
                r.getDepartment().getId(), COMMITTED, yearStart, r.getId());
        BigDecimal budget = r.getDepartment().getAnnualBudget();
        if (committed.add(r.getTotalAmount()).compareTo(budget) > 0) {
            throw new ConflictException("Department budget exceeded: budget " + budget + ", already committed "
                    + committed + ", this requisition " + r.getTotalAmount() + ".");
        }
    }
}
