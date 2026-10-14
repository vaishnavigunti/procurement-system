package com.infosys.procurement_system.requisition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequisitionResponse(Long id, String requisitionNumber, String title, String justification,
                                  Long requesterId, String requesterName, Long departmentId, String departmentName,
                                  Priority priority, RequisitionStatus status, BigDecimal totalAmount,
                                  LocalDate requiredByDate, Long workflowId, String workflowName,
                                  Integer currentStep, Instant submittedAt, Instant createdAt,
                                  String cancelReason, List<RequisitionItemResponse> items) {

    public static RequisitionResponse from(Requisition r) {
        return new RequisitionResponse(
                r.getId(),
                r.getRequisitionNumber(),
                r.getTitle(),
                r.getJustification(),
                r.getRequester().getId(),
                r.getRequester().getFullName(),
                r.getDepartment().getId(),
                r.getDepartment().getName(),
                r.getPriority(),
                r.getStatus(),
                r.getTotalAmount(),
                r.getRequiredByDate(),
                r.getWorkflow() == null ? null : r.getWorkflow().getId(),
                r.getWorkflow() == null ? null : r.getWorkflow().getName(),
                r.getCurrentStep(),
                r.getSubmittedAt(),
                r.getCreatedAt(),
                r.getCancelReason(),
                r.getItems().stream().map(RequisitionItemResponse::from).toList());
    }
}
