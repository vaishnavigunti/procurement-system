package com.infosys.procurement_system.supplier;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.category.Category;
import com.infosys.procurement_system.category.CategoryRepository;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.BadRequestException;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplierService {

    private static final String TYPE = "SUPPLIER";

    /** Allowed status changes. BLACKLISTED is final. */
    private static final Map<SupplierStatus, Set<SupplierStatus>> TRANSITIONS = Map.of(
            SupplierStatus.PENDING, EnumSet.of(SupplierStatus.ACTIVE, SupplierStatus.BLACKLISTED),
            SupplierStatus.ACTIVE, EnumSet.of(SupplierStatus.SUSPENDED, SupplierStatus.BLACKLISTED),
            SupplierStatus.SUSPENDED, EnumSet.of(SupplierStatus.ACTIVE, SupplierStatus.BLACKLISTED),
            SupplierStatus.BLACKLISTED, EnumSet.noneOf(SupplierStatus.class));

    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final AuditService auditService;

    public PageResponse<SupplierResponse> list(SupplierStatus status, Long categoryId, String q, Pageable pageable) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                if (query != null) {
                    query.distinct(true);
                }
                predicates.add(cb.equal(root.join("categories").get("id"), categoryId));
            }
            if (q != null && !q.isBlank()) {
                String like = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("supplierCode")), like),
                        cb.like(cb.lower(root.get("taxId")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(supplierRepository.findAll(spec, pageable), SupplierResponse::from);
    }

    public SupplierResponse get(Long id) {
        return SupplierResponse.from(find(id));
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        String taxId = request.taxId().trim().toUpperCase();
        if (supplierRepository.existsByTaxIdIgnoreCase(taxId)) {
            throw new ConflictException("A supplier with this tax ID already exists.");
        }
        Supplier supplier = new Supplier();
        supplier.setSupplierCode(String.format("SUP-%05d", supplierRepository.nextSupplierNumber()));
        apply(supplier, request, taxId);
        Supplier saved = supplierRepository.save(supplier);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED, "Registered supplier " + saved.getSupplierCode());
        return SupplierResponse.from(saved);
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = find(id);
        if (supplier.getStatus() == SupplierStatus.BLACKLISTED) {
            throw new ConflictException("A blacklisted supplier cannot be edited.");
        }
        String taxId = request.taxId().trim().toUpperCase();
        if (supplierRepository.existsByTaxIdIgnoreCaseAndIdNot(taxId, id)) {
            throw new ConflictException("A supplier with this tax ID already exists.");
        }
        apply(supplier, request, taxId);
        auditService.record(TYPE, id, AuditAction.UPDATED, "Updated supplier " + supplier.getSupplierCode());
        return SupplierResponse.from(supplier);
    }

    @Transactional
    public SupplierResponse changeStatus(Long id, SupplierStatusRequest request) {
        Supplier supplier = find(id);
        SupplierStatus from = supplier.getStatus();
        SupplierStatus to = request.status();
        if (from == to) {
            throw new ConflictException("Supplier is already " + to + ".");
        }
        if (!TRANSITIONS.get(from).contains(to)) {
            throw new ConflictException("Cannot change supplier status from " + from + " to " + to + ".");
        }
        boolean needsReason = to == SupplierStatus.SUSPENDED || to == SupplierStatus.BLACKLISTED;
        String reason = request.reason() == null ? null : request.reason().trim();
        if (needsReason && (reason == null || reason.isEmpty())) {
            throw new BadRequestException("A reason is required to set status " + to + ".");
        }
        if (to == SupplierStatus.ACTIVE && supplier.getComplianceStatus() != ComplianceStatus.COMPLIANT) {
            throw new ConflictException("A supplier can be activated only when its compliance status is COMPLIANT.");
        }
        supplier.setStatus(to);
        supplier.setStatusReason(reason == null || reason.isEmpty() ? null : reason);
        supplier.setActive(to != SupplierStatus.BLACKLISTED);
        auditService.record(TYPE, id, AuditAction.STATUS_CHANGED,
                from + " -> " + to + (reason == null || reason.isEmpty() ? "" : ": " + reason));
        return SupplierResponse.from(supplier);
    }

    @Transactional
    public SupplierResponse evaluate(Long id, SupplierEvaluationRequest request) {
        if (request.performanceRating() == null && request.complianceStatus() == null) {
            throw new BadRequestException("Provide performanceRating, complianceStatus or both.");
        }
        Supplier supplier = find(id);
        StringBuilder details = new StringBuilder();
        if (request.performanceRating() != null) {
            supplier.setPerformanceRating(request.performanceRating());
            details.append("rating=").append(request.performanceRating()).append(' ');
        }
        if (request.complianceStatus() != null) {
            supplier.setComplianceStatus(request.complianceStatus());
            details.append("compliance=").append(request.complianceStatus()).append(' ');
            if (request.complianceStatus() == ComplianceStatus.NON_COMPLIANT
                    && supplier.getStatus() == SupplierStatus.ACTIVE) {
                supplier.setStatus(SupplierStatus.SUSPENDED);
                supplier.setStatusReason("Automatically suspended: marked NON_COMPLIANT.");
                details.append("(auto-suspended)");
            }
        }
        auditService.record(TYPE, id, AuditAction.UPDATED, "Evaluation: " + details.toString().trim());
        return SupplierResponse.from(supplier);
    }

    private void apply(Supplier supplier, SupplierRequest request, String taxId) {
        supplier.setName(request.name().trim());
        supplier.setContactPerson(request.contactPerson());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setTaxId(taxId);
        supplier.setCategories(loadCategories(request.categoryIds()));
    }

    private Set<Category> loadCategories(Set<Long> ids) {
        List<Category> found = categoryRepository.findAllById(ids);
        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException("One or more categories were not found.");
        }
        if (found.stream().anyMatch(c -> !c.isActive())) {
            throw new ConflictException("Inactive categories cannot be assigned to a supplier.");
        }
        return new HashSet<>(found);
    }

    private Supplier find(Long id) {
        return supplierRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Supplier", id));
    }
}
