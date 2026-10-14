package com.infosys.procurement_system.approvalworkflow;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.role.Role;
import com.infosys.procurement_system.role.RoleRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalWorkflowService {

    private static final String TYPE = "APPROVAL_WORKFLOW";

    private final ApprovalWorkflowRepository workflowRepository;
    private final ApprovalWorkflowStepRepository stepRepository;
    private final RoleRepository roleRepository;
    private final AuditService auditService;

    public PageResponse<ApprovalWorkflowResponse> list(Boolean active, Pageable pageable) {
        Page<ApprovalWorkflow> page = active == null
                ? workflowRepository.findAll(pageable)
                : workflowRepository.findAll((root, query, cb) -> cb.equal(root.get("active"), active), pageable);
        return PageResponse.from(page, ApprovalWorkflowResponse::from);
    }

    public ApprovalWorkflowResponse get(Long id) {
        return ApprovalWorkflowResponse.from(find(id));
    }

    @Transactional
    public ApprovalWorkflowResponse create(ApprovalWorkflowRequest request) {
        String name = request.name().trim();
        if (workflowRepository.existsByName(name)) {
            throw new ConflictException("A workflow with this name already exists.");
        }
        validateSteps(request.steps());
        ApprovalWorkflow workflow = new ApprovalWorkflow();
        workflow.setName(name);
        workflow.setMinAmount(request.minAmount());
        workflow.setMaxAmount(request.maxAmount());
        ApprovalWorkflow saved = workflowRepository.save(workflow);
        List<ApprovalWorkflowStep> steps = saveSteps(saved, request.steps());
        saved.setSteps(steps);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED, "Created workflow " + saved.getName());
        return ApprovalWorkflowResponse.from(saved);
    }

    @Transactional
    public ApprovalWorkflowResponse update(Long id, ApprovalWorkflowRequest request) {
        ApprovalWorkflow workflow = find(id);
        String name = request.name().trim();
        if (workflowRepository.existsByNameAndIdNot(name, id)) {
            throw new ConflictException("A workflow with this name already exists.");
        }
        validateSteps(request.steps());
        workflow.setName(name);
        workflow.setMinAmount(request.minAmount());
        workflow.setMaxAmount(request.maxAmount());
        stepRepository.deleteAll(workflow.getSteps());
        List<ApprovalWorkflowStep> steps = saveSteps(workflow, request.steps());
        workflow.setSteps(steps);
        auditService.record(TYPE, id, AuditAction.UPDATED, "Updated workflow " + name);
        return ApprovalWorkflowResponse.from(workflow);
    }

    @Transactional
    public ApprovalWorkflowResponse setActive(Long id, boolean active) {
        ApprovalWorkflow workflow = find(id);
        workflow.setActive(active);
        auditService.record(TYPE, id, AuditAction.STATUS_CHANGED, active ? "Activated" : "Deactivated");
        return ApprovalWorkflowResponse.from(workflow);
    }

    private void validateSteps(List<ApprovalWorkflowStepRequest> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new ConflictException("A workflow must have at least one step.");
        }
        List<Integer> orders = new ArrayList<>(steps.stream().map(ApprovalWorkflowStepRequest::stepOrder).toList());
        orders.sort(Comparator.naturalOrder());
        for (int i = 0; i < orders.size(); i++) {
            if (orders.get(i) != i + 1) {
                throw new ConflictException("Step orders must be sequential starting from 1.");
            }
        }
    }

    private List<ApprovalWorkflowStep> saveSteps(ApprovalWorkflow workflow, List<ApprovalWorkflowStepRequest> requests) {
        List<ApprovalWorkflowStep> steps = new ArrayList<>();
        for (ApprovalWorkflowStepRequest req : requests) {
            ApprovalWorkflowStep step = new ApprovalWorkflowStep();
            step.setWorkflow(workflow);
            step.setStepOrder(req.stepOrder());
            Role role = roleRepository.findById(req.approverRoleId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Role", req.approverRoleId()));
            step.setApproverRole(role);
            steps.add(stepRepository.save(step));
        }
        return steps;
    }

    private ApprovalWorkflow find(Long id) {
        return workflowRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("ApprovalWorkflow", id));
    }
}
