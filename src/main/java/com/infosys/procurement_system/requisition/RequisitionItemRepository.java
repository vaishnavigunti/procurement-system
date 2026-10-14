package com.infosys.procurement_system.requisition;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisitionItemRepository extends JpaRepository<RequisitionItem, Long> {

    List<RequisitionItem> findByRequisitionId(Long requisitionId);
}
