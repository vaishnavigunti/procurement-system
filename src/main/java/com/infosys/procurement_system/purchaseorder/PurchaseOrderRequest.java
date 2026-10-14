package com.infosys.procurement_system.purchaseorder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PurchaseOrderRequest(
        @NotNull Long requisitionId,
        @NotNull Long supplierId,
        LocalDate expectedDeliveryDate,
        @Size(max = 100) String paymentTerms,
        @Size(max = 1000) String deliveryAddress) {
}
