package com.infosys.procurement_system.approvalworkflow;

import java.math.BigDecimal;
import java.util.List;

public record ApprovalWorkflowResponse(Long id, String name, BigDecimal minAmount, BigDecimal maxAmount,
                                       boolean active, List<ApprovalWorkflowStepResponse> steps) {

    public static ApprovalWorkflowResponse from(ApprovalWorkflow workflow) {
        return new ApprovalWorkflowResponse(
                workflow.getId(),
                workflow.getName(),
                workflow.getMinAmount(),
                workflow.getMaxAmount(),
                workflow.isActive(),
                workflow.getSteps().stream().map(ApprovalWorkflowStepResponse::from).toList());
    }
}
