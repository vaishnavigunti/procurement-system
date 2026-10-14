package com.infosys.procurement_system.purchaseorder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PurchaseOrderCancelRequest(@NotBlank @Size(max = 500) String reason) {
}
