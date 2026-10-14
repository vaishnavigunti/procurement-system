package com.infosys.procurement_system.requisition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record RequisitionRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String justification,
        @NotNull Priority priority,
        LocalDate requiredByDate,
        @NotNull List<RequisitionItemRequest> items) {
}
