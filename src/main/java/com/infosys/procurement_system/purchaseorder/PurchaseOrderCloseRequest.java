package com.infosys.procurement_system.purchaseorder;

import jakarta.validation.constraints.Size;

public record PurchaseOrderCloseRequest(@Size(max = 500) String comments) {
}
