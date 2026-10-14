package com.infosys.procurement_system.purchaseorder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(Long id, String poNumber, Long requisitionId, String requisitionNumber,
                                    Long supplierId, String supplierName, PurchaseOrderStatus status,
                                    LocalDate orderDate, LocalDate expectedDeliveryDate,
                                    LocalDate confirmedDeliveryDate, BigDecimal totalAmount,
                                    String paymentTerms, String deliveryAddress, String createdByName,
                                    String cancelReason, String closureComments, int fulfillmentPercent,
                                    Instant createdAt, List<PurchaseOrderItemResponse> items) {

    public static PurchaseOrderResponse from(PurchaseOrder po) {
        long ordered = po.getItems().stream().mapToLong(PurchaseOrderItem::getOrderedQuantity).sum();
        long received = po.getItems().stream().mapToLong(PurchaseOrderItem::getReceivedQuantity).sum();
        int percent = ordered == 0 ? 0 : (int) (received * 100 / ordered);
        return new PurchaseOrderResponse(
                po.getId(),
                po.getPoNumber(),
                po.getRequisition().getId(),
                po.getRequisition().getRequisitionNumber(),
                po.getSupplier().getId(),
                po.getSupplier().getName(),
                po.getStatus(),
                po.getOrderDate(),
                po.getExpectedDeliveryDate(),
                po.getConfirmedDeliveryDate(),
                po.getTotalAmount(),
                po.getPaymentTerms(),
                po.getDeliveryAddress(),
                po.getCreatedBy().getFullName(),
                po.getCancelReason(),
                po.getClosureComments(),
                percent,
                po.getCreatedAt(),
                po.getItems().stream().map(PurchaseOrderItemResponse::from).toList());
    }
}
