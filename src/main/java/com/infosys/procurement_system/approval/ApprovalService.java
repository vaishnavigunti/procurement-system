package com.infosys.procurement_system.approval;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.common.CurrentUserProvider;
import com.infosys.procurement_system.common.exception.BadRequestException;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ForbiddenOperationException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.requisition.Requisition;
import com.infosys.procurement_system.requisition.RequisitionRepository;
import com.infosys.procurement_system.requisition.RequisitionResponse;
import com.infosys.procurement_system.requisition.RequisitionStatus;
import com.infosys.procurement_system.user.User;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Approve, reject or return a requisition at its current step, plus the approver's inbox. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private static final String TYPE = "REQUISITION";
    private static final Set<String> DEPARTMENT_SCOPED = Set.of("MANAGER", "DEPARTMENT_HEAD");

    private final ApprovalRepository approvalRepository;
    private final RequisitionRepository requisitionRepository;
    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    /** Approvals waiting for the current user: their role, the live step, same department when scoped. */
    public List<ApprovalResponse> pendingForMe() {
        User me = currentUserProvider.requireCurrentUser();
        return approvalRepository.findPendingByApproverRole(me.getId()).stream()
                .filter(a -> a.getRequisition().getStatus() == RequisitionStatus.PENDING_APPROVAL)
                .filter(a -> !a.getRequisition().getRequester().getId().equals(me.getId()))
                .filter(a -> departmentAllowed(me, a.getRequisition()))
                .filter(this::isLiveStep)
                .map(ApprovalResponse::from)
                .toList();
    }

    @Transactional
    public RequisitionResponse approve(Long requisitionId, ApprovalDecisionRequest request) {
        Requisition r = findPending(requisitionId);
        User me = currentUserProvider.requireCurrentUser();
        Approval current = liveApproval(r);
        assertMayDecide(me, r, current);
        record(current, me, ApprovalStatus.APPROVED, request);

        var next = approvalRepository.findNextPendingApproval(r.getId(), current.getRoundNumber());
        if (next.isPresent()) {
            r.setCurrentStep(next.get().getWorkflowStep().getStepOrder());
            audit(r, AuditAction.APPROVED, "Step " + current.getWorkflowStep().getStepOrder() + " approved by "
                    + me.getFullName() + "; waiting for step " + r.getCurrentStep());
        } else {
            r.setStatus(RequisitionStatus.APPROVED);
            audit(r, AuditAction.APPROVED, "Final approval by " + me.getFullName() + ": requisition fully approved");
        }
        return RequisitionResponse.from(r);
    }

    @Transactional
    public RequisitionResponse reject(Long requisitionId, ApprovalDecisionRequest request) {
        return stop(requisitionId, request, ApprovalStatus.REJECTED, RequisitionStatus.REJECTED, AuditAction.REJECTED);
    }

    @Transactional
    public RequisitionResponse returnToRequester(Long requisitionId, ApprovalDecisionRequest request) {
        return stop(requisitionId, request, ApprovalStatus.RETURNED, RequisitionStatus.RETURNED, AuditAction.RETURNED);
    }

    // ---------------------------------------------------------------- helpers

    private RequisitionResponse stop(Long requisitionId, ApprovalDecisionRequest request,
                                     ApprovalStatus approvalStatus, RequisitionStatus requisitionStatus,
                                     AuditAction action) {
        if (request == null || request.comments() == null || request.comments().isBlank()) {
            throw new BadRequestException("Comments are required to reject or return a requisition.");
        }
        Requisition r = findPending(requisitionId);
        User me = currentUserProvider.requireCurrentUser();
        Approval current = liveApproval(r);
        assertMayDecide(me, r, current);
        record(current, me, approvalStatus, request);

        for (Approval a : approvalRepository.findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(r.getId())) {
            if (a.getRoundNumber().equals(current.getRoundNumber()) && a.getStatus() == ApprovalStatus.PENDING) {
                a.setStatus(ApprovalStatus.CANCELLED);
            }
        }
        r.setStatus(requisitionStatus);
        audit(r, action, approvalStatus + " at step " + current.getWorkflowStep().getStepOrder() + " by "
                + me.getFullName() + ": " + request.comments().trim());
        return RequisitionResponse.from(r);
    }

    private Requisition findPending(Long id) {
        Requisition r = requisitionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Requisition", id));
        if (r.getStatus() != RequisitionStatus.PENDING_APPROVAL) {
            throw new ConflictException("Requisition is " + r.getStatus() + ", not PENDING_APPROVAL.");
        }
        return r;
    }

    private Approval liveApproval(Requisition r) {
        List<Approval> all = approvalRepository
                .findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(r.getId());
        int round = all.isEmpty() ? 1 : all.get(all.size() - 1).getRoundNumber();
        return approvalRepository.findNextPendingApproval(r.getId(), round)
                .orElseThrow(() -> new ConflictException("This requisition has no pending approval step."));
    }

    private boolean isLiveStep(Approval a) {
        return approvalRepository.findNextPendingApproval(a.getRequisition().getId(), a.getRoundNumber())
                .map(live -> live.getId().equals(a.getId()))
                .orElse(false);
    }

    private boolean departmentAllowed(User me, Requisition r) {
        return !DEPARTMENT_SCOPED.contains(me.getRole().getName())
                || r.getDepartment().getId().equals(me.getDepartment().getId());
    }

    private void assertMayDecide(User me, Requisition r, Approval current) {
        if (r.getRequester().getId().equals(me.getId())) {
            throw new ForbiddenOperationException("You cannot decide on your own requisition.");
        }
        String needed = current.getWorkflowStep().getApproverRole().getName();
        if (!current.getWorkflowStep().getApproverRole().getId().equals(me.getRole().getId())) {
            throw new ForbiddenOperationException("The current step needs the role " + needed + ".");
        }
        if (!departmentAllowed(me, r)) {
            throw new ForbiddenOperationException("You can only decide on requisitions from your own department.");
        }
    }

    private void record(Approval approval, User me, ApprovalStatus status, ApprovalDecisionRequest request) {
        approval.setStatus(status);
        approval.setDecidedBy(me);
        approval.setDecidedAt(Instant.now());
        approval.setComments(request == null || request.comments() == null ? null : request.comments().trim());
    }

    private void audit(Requisition r, AuditAction action, String details) {
        auditService.record(TYPE, r.getId(), action, r.getRequisitionNumber() + ": " + details);
    }
}
