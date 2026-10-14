package com.infosys.procurement_system.purchaseorder;

import com.infosys.procurement_system.requisition.RequisitionItem;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "po_items")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_item_id", nullable = false)
    private RequisitionItem requisitionItem;

    @Column(name = "ordered_quantity", nullable = false)
    private Integer orderedQuantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "received_quantity", nullable = false)
    private Integer receivedQuantity = 0;
}
