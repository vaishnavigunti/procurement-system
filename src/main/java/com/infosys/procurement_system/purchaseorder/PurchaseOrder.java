package com.infosys.procurement_system.purchaseorder;

import com.infosys.procurement_system.common.BaseEntity;
import com.infosys.procurement_system.requisition.Requisition;
import com.infosys.procurement_system.supplier.Supplier;
import com.infosys.procurement_system.user.User;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
public class PurchaseOrder extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 30)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requisition_id", nullable = false)
    private Requisition requisition;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status = PurchaseOrderStatus.CREATED;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate = LocalDate.now();

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "confirmed_delivery_date")
    private LocalDate confirmedDeliveryDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "payment_terms", length = 100)
    private String paymentTerms;

    @Column(name = "delivery_address", columnDefinition = "text")
    private String deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "closure_comments", length = 500)
    private String closureComments;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<PurchaseOrderItem> items = new ArrayList<>();
}
