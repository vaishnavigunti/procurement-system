package com.infosys.procurement_system.approvalworkflow;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalWorkflowStepRepository extends JpaRepository<ApprovalWorkflowStep, Long> {

    List<ApprovalWorkflowStep> findByWorkflowIdOrderByStepOrderAsc(Long workflowId);
}
