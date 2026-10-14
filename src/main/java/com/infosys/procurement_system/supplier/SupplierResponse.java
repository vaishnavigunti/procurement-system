package com.infosys.procurement_system.supplier;

import com.infosys.procurement_system.category.Category;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

public record SupplierResponse(Long id, String supplierCode, String name, String contactPerson, String email,
                               String phone, String address, String taxId, SupplierStatus status,
                               ComplianceStatus complianceStatus, BigDecimal performanceRating, String statusReason,
                               boolean active, List<CategoryRef> categories, Instant createdAt) {

    public record CategoryRef(Long id, String name) {
    }

    public static SupplierResponse from(Supplier s) {
        List<CategoryRef> categories = s.getCategories().stream()
                .sorted(Comparator.comparing(Category::getName))
                .map(c -> new CategoryRef(c.getId(), c.getName()))
                .toList();
        return new SupplierResponse(s.getId(), s.getSupplierCode(), s.getName(), s.getContactPerson(), s.getEmail(),
                s.getPhone(), s.getAddress(), s.getTaxId(), s.getStatus(), s.getComplianceStatus(),
                s.getPerformanceRating(), s.getStatusReason(), s.isActive(), categories, s.getCreatedAt());
    }
}
