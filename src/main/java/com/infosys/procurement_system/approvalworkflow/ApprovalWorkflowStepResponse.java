package com.infosys.procurement_system.approvalworkflow;

public record ApprovalWorkflowStepResponse(Long id, Integer stepOrder, Long approverRoleId, String approverRoleName) {

    public static ApprovalWorkflowStepResponse from(ApprovalWorkflowStep step) {
        return new ApprovalWorkflowStepResponse(
                step.getId(),
                step.getStepOrder(),
                step.getApproverRole().getId(),
                step.getApproverRole().getName());
    }
}
