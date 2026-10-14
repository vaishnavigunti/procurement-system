package com.infosys.procurement_system.requisition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RequisitionItemRequest(
        @NotNull Long categoryId,
        @NotBlank @Size(max = 200) String itemName,
        @Size(max = 500) String description,
        @NotNull @Positive Integer quantity,
        @NotBlank @Size(max = 20) String unit,
        @NotNull @PositiveOrZero BigDecimal estimatedUnitPrice) {
}
