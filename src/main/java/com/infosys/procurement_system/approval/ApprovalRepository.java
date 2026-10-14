package com.infosys.procurement_system.approval;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByRequisitionIdOrderByRoundNumberAscWorkflowStepStepOrderAsc(Long requisitionId);

    @Query("""
            SELECT a FROM Approval a
            WHERE a.requisition.id = :requisitionId
              AND a.roundNumber = :roundNumber
              AND a.status = 'PENDING'
            ORDER BY a.workflowStep.stepOrder ASC
            LIMIT 1
            """)
    Optional<Approval> findNextPendingApproval(@Param("requisitionId") Long requisitionId,
                                               @Param("roundNumber") Integer roundNumber);

    @Query("""
            SELECT a FROM Approval a
            JOIN a.workflowStep ws
            WHERE a.status = 'PENDING'
              AND ws.approverRole.id IN (
                  SELECT u.role.id FROM User u WHERE u.id = :userId
              )
            ORDER BY a.requisition.submittedAt ASC
            """)
    List<Approval> findPendingByApproverRole(@Param("userId") Long userId);

    long countByRequisitionIdAndRoundNumberAndStatus(Long requisitionId, Integer roundNumber, ApprovalStatus status);
}
