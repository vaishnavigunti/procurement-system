package com.infosys.procurement_system.requisition;

import java.math.BigDecimal;

public record RequisitionItemResponse(Long id, Long categoryId, String categoryName, String itemName,
                                      String description, Integer quantity, String unit,
                                      BigDecimal estimatedUnitPrice, BigDecimal totalPrice) {

    public static RequisitionItemResponse from(RequisitionItem item) {
        return new RequisitionItemResponse(
                item.getId(),
                item.getCategory().getId(),
                item.getCategory().getName(),
                item.getItemName(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnit(),
                item.getEstimatedUnitPrice(),
                item.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
