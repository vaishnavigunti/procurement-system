package com.infosys.procurement_system.approvalworkflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ApprovalWorkflowRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @PositiveOrZero BigDecimal minAmount,
        BigDecimal maxAmount,
        @NotNull List<ApprovalWorkflowStepRequest> steps) {
}
