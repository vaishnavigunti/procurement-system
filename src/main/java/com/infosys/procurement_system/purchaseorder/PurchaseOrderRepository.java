package com.infosys.procurement_system.purchaseorder;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseOrderRepository
        extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {

    /** True when the requisition already has a live (not cancelled) purchase order. */
    boolean existsByRequisitionIdAndStatusNot(Long requisitionId, PurchaseOrderStatus status);

    @Query(value = "select nextval('purchase_order_number_seq')", nativeQuery = true)
    Long nextPurchaseOrderNumber();
}
