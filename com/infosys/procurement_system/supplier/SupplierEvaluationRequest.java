package com.infosys.procurement_system.supplier;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;

/** Send performanceRating, complianceStatus or both. */
public record SupplierEvaluationRequest(
        @DecimalMin("0.0") @DecimalMax("5.0") BigDecimal performanceRating,
        ComplianceStatus complianceStatus) {
}
