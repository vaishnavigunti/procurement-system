package com.infosys.procurement_system.requisition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequisitionSubmitRequest(
        @NotBlank @Size(max = 2000) String justification) {
}
