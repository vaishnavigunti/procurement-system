package com.infosys.procurement_system.purchaseorder;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PurchaseOrderConfirmRequest(@NotNull LocalDate confirmedDeliveryDate) {
}
