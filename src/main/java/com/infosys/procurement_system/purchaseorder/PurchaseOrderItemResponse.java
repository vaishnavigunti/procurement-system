package com.infosys.procurement_system.purchaseorder;

import java.math.BigDecimal;

public record PurchaseOrderItemResponse(Long id, Long requisitionItemId, String itemName, String unit,
                                        Integer orderedQuantity, Integer receivedQuantity,
                                        BigDecimal unitPrice, BigDecimal lineTotal) {
    public static PurchaseOrderItemResponse from(PurchaseOrderItem i) {
        return new PurchaseOrderItemResponse(
                i.getId(),
                i.getRequisitionItem().getId(),
                i.getRequisitionItem().getItemName(),
                i.getRequisitionItem().getUnit(),
                i.getOrderedQuantity(),
                i.getReceivedQuantity(),
                i.getUnitPrice(),
                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getOrderedQuantity())));
    }
}
