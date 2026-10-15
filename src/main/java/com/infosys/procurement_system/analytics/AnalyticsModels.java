package com.infosys.procurement_system.analytics;

import java.math.BigDecimal;
import java.util.Map;

/** Response shapes for the analytics endpoints. */
public final class AnalyticsModels {

    private AnalyticsModels() {
    }

    public record Summary(Map<String, Long> requisitionsByStatus, Map<String, Long> purchaseOrdersByStatus,
                          long awaitingApproval, long activeSuppliers,
                          BigDecimal totalPurchaseOrderValue, BigDecimal approvedNotYetOrderedValue) {
    }

    public record DepartmentSpend(Long departmentId, String department, BigDecimal annualBudget,
                                  BigDecimal spent, BigDecimal utilisationPercent) {
    }

    public record CategorySpend(Long categoryId, String category, BigDecimal spent) {
    }

    public record SupplierSpend(Long supplierId, String supplier, BigDecimal performanceRating,
                                String complianceStatus, long purchaseOrders, BigDecimal spent) {
    }

    public record MonthlySpend(String month, long purchaseOrders, BigDecimal spent) {
    }

    public record ApproverPerformance(String role, long decisions, long approved, long rejected, long returned,
                                      Double averageHoursFromSubmission) {
    }
}
