package com.infosys.procurement_system.requisition;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface RequisitionRepository extends JpaRepository<Requisition, Long>, JpaSpecificationExecutor<Requisition> {

    boolean existsByRequisitionNumber(String requisitionNumber);

    Optional<Requisition> findByRequisitionNumber(String requisitionNumber);

    @Query(value = "select nextval('requisition_number_seq')", nativeQuery = true)
    Long nextRequisitionNumber();
}
