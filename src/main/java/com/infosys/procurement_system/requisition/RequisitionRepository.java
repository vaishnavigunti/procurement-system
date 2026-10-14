package com.infosys.procurement_system.requisition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisitionRepository extends JpaRepository<Requisition, Long>, JpaSpecificationExecutor<Requisition> {

    boolean existsByRequisitionNumber(String requisitionNumber);

    Optional<Requisition> findByRequisitionNumber(String requisitionNumber);

    @Query(value = "select nextval('requisition_number_seq')", nativeQuery = true)
    Long nextRequisitionNumber();

    /** Money already committed by a department: submitted/approved requisitions since a date (budget check). */
    @Query("""
            SELECT COALESCE(SUM(r.totalAmount), 0) FROM Requisition r
            WHERE r.department.id = :departmentId
              AND r.status IN :statuses
              AND r.submittedAt >= :since
              AND r.id <> :excludeId
            """)
    BigDecimal sumCommittedAmount(@Param("departmentId") Long departmentId,
                                  @Param("statuses") Collection<RequisitionStatus> statuses,
                                  @Param("since") Instant since,
                                  @Param("excludeId") Long excludeId);
}
