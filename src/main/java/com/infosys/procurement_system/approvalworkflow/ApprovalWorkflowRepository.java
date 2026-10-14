package com.infosys.procurement_system.approvalworkflow;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long>, JpaSpecificationExecutor<ApprovalWorkflow> {

    List<ApprovalWorkflow> findByActiveTrue();

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * Find the first active workflow where minAmount <= amount < maxAmount.
     * maxAmount NULL means no upper limit.
     */
    @Query("""
            SELECT w FROM ApprovalWorkflow w
            WHERE w.active = true
              AND w.minAmount <= :amount
              AND (w.maxAmount IS NULL OR w.maxAmount > :amount)
            ORDER BY w.minAmount ASC
            LIMIT 1
            """)
    ApprovalWorkflow findMatchingWorkflow(@Param("amount") BigDecimal amount);
}
