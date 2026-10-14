package com.infosys.procurement_system.requisition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisitionCancelRequest(@NotBlank @Size(max = 500) String reason) {
}
