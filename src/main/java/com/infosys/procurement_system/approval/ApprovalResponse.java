package com.infosys.procurement_system.approval;

import java.math.BigDecimal;
import java.time.Instant;

public record ApprovalResponse(Long id, Long requisitionId, String requisitionNumber, String title,
                               BigDecimal totalAmount, String requesterName, String departmentName,
                               Integer roundNumber, Integer stepOrder, String approverRole,
                               ApprovalStatus status, String decidedByName, String comments, Instant decidedAt) {

    public static ApprovalResponse from(Approval a) {
        return new ApprovalResponse(
                a.getId(),
                a.getRequisition().getId(),
                a.getRequisition().getRequisitionNumber(),
                a.getRequisition().getTitle(),
                a.getRequisition().getTotalAmount(),
                a.getRequisition().getRequester().getFullName(),
                a.getRequisition().getDepartment().getName(),
                a.getRoundNumber(),
                a.getWorkflowStep().getStepOrder(),
                a.getWorkflowStep().getApproverRole().getName(),
                a.getStatus(),
                a.getDecidedBy() == null ? null : a.getDecidedBy().getFullName(),
                a.getComments(),
                a.getDecidedAt());
    }
}
