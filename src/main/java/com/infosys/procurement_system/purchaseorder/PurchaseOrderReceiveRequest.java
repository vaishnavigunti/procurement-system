package com.infosys.procurement_system.purchaseorder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/** Goods received: a quantity for each purchase-order item that arrived. */
public record PurchaseOrderReceiveRequest(@NotEmpty @Valid List<Line> lines) {

    public record Line(@NotNull Long itemId, @NotNull @Positive Integer quantity) {
    }
}
