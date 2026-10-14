package com.infosys.procurement_system.approvalworkflow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ApprovalWorkflowStepRequest(
        @NotNull @Positive Integer stepOrder,
        @NotNull Long approverRoleId) {
}
