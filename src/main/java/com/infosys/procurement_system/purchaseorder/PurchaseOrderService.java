package com.infosys.procurement_system.purchaseorder;

import com.infosys.procurement_system.audit.AuditAction;
import com.infosys.procurement_system.audit.AuditService;
import com.infosys.procurement_system.category.Category;
import com.infosys.procurement_system.common.CurrentUserProvider;
import com.infosys.procurement_system.common.PageResponse;
import com.infosys.procurement_system.common.exception.BadRequestException;
import com.infosys.procurement_system.common.exception.ConflictException;
import com.infosys.procurement_system.common.exception.ForbiddenOperationException;
import com.infosys.procurement_system.common.exception.ResourceNotFoundException;
import com.infosys.procurement_system.requisition.Requisition;
import com.infosys.procurement_system.requisition.RequisitionItem;
import com.infosys.procurement_system.requisition.RequisitionRepository;
import com.infosys.procurement_system.requisition.RequisitionStatus;
import com.infosys.procurement_system.supplier.ComplianceStatus;
import com.infosys.procurement_system.supplier.Supplier;
import com.infosys.procurement_system.supplier.SupplierRepository;
import com.infosys.procurement_system.supplier.SupplierStatus;
import com.infosys.procurement_system.user.User;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Purchase order lifecycle: create from an approved requisition, issue, confirm, receive goods, close or cancel. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderService {

    private static final String TYPE = "PURCHASE_ORDER";
    private static final Set<String> BUYERS = Set.of("PROCUREMENT_OFFICER", "ADMIN");
    private static final Set<String> SEES_ALL = Set.of("ADMIN", "FINANCE_HEAD", "PROCUREMENT_OFFICER");
    private static final Set<String> SEES_DEPARTMENT = Set.of("MANAGER", "DEPARTMENT_HEAD");

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RequisitionRepository requisitionRepository;
    private final SupplierRepository supplierRepository;
    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    public PageResponse<PurchaseOrderResponse> list(PurchaseOrderStatus status, Long supplierId, Pageable pageable) {
        User me = currentUserProvider.requireCurrentUser();
        String role = me.getRole().getName();
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            Predicate mine = cb.equal(root.get("requisition").get("requester").get("id"), me.getId());
            if (SEES_DEPARTMENT.contains(role)) {
                predicates.add(cb.or(mine,
                        cb.equal(root.get("requisition").get("department").get("id"), me.getDepartment().getId())));
            } else if (!SEES_ALL.contains(role)) {
                predicates.add(mine);
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return PageResponse.from(purchaseOrderRepository.findAll(spec, pageable), PurchaseOrderResponse::from);
    }

    public PurchaseOrderResponse get(Long id) {
        PurchaseOrder po = find(id);
        assertCanView(currentUserProvider.requireCurrentUser(), po);
        return PurchaseOrderResponse.from(po);
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        User me = currentUserProvider.requireCurrentUser();
        requireBuyer(me);

        Requisition requisition = requisitionRepository.findById(request.requisitionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Requisition", request.requisitionId()));
        if (requisition.getStatus() != RequisitionStatus.APPROVED) {
            throw new ConflictException("Only APPROVED requisitions can become purchase orders (this one is "
                    + requisition.getStatus() + ").");
        }
        if (purchaseOrderRepository.existsByRequisitionIdAndStatusNot(requisition.getId(), PurchaseOrderStatus.CANCELLED)) {
            throw new ConflictException("This requisition already has a purchase order.");
        }
        if (request.expectedDeliveryDate() != null && request.expectedDeliveryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Expected delivery date cannot be in the past.");
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> ResourceNotFoundException.of("Supplier", request.supplierId()));
        checkSupplier(supplier, requisition);

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(String.format("PO-%d-%05d", Year.now().getValue(),
                purchaseOrderRepository.nextPurchaseOrderNumber()));
        po.setRequisition(requisition);
        po.setSupplier(supplier);
        po.setCreatedBy(me);
        po.setOrderDate(LocalDate.now());
        po.setExpectedDeliveryDate(request.expectedDeliveryDate());
        po.setPaymentTerms(request.paymentTerms());
        po.setDeliveryAddress(request.deliveryAddress());

        BigDecimal total = BigDecimal.ZERO;
        for (RequisitionItem item : requisition.getItems()) {
            PurchaseOrderItem line = new PurchaseOrderItem();
            line.setPurchaseOrder(po);
            line.setRequisitionItem(item);
            line.setOrderedQuantity(item.getQuantity());
            line.setUnitPrice(item.getEstimatedUnitPrice());
            po.getItems().add(line);
            total = total.add(item.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        po.setTotalAmount(total);
        PurchaseOrder saved = purchaseOrderRepository.save(po);

        requisition.setStatus(RequisitionStatus.CONVERTED);
        auditService.record(TYPE, saved.getId(), AuditAction.CREATED,
                saved.getPoNumber() + " created from " + requisition.getRequisitionNumber()
                        + " for supplier " + supplier.getName() + ", total " + total);
        auditService.record("REQUISITION", requisition.getId(), AuditAction.STATUS_CHANGED,
                requisition.getRequisitionNumber() + " converted to " + saved.getPoNumber());
        return PurchaseOrderResponse.from(saved);
    }

    @Transactional
    public PurchaseOrderResponse issue(Long id) {
        PurchaseOrder po = buyerOrder(id);
        requireStatus(po, "issued", PurchaseOrderStatus.CREATED);
        po.setStatus(PurchaseOrderStatus.ISSUED);
        auditService.record(TYPE, id, AuditAction.ISSUED, po.getPoNumber() + " issued to " + po.getSupplier().getName());
        return PurchaseOrderResponse.from(po);
    }

    @Transactional
    public PurchaseOrderResponse confirm(Long id, PurchaseOrderConfirmRequest request) {
        PurchaseOrder po = buyerOrder(id);
        requireStatus(po, "confirmed", PurchaseOrderStatus.ISSUED);
        if (request.confirmedDeliveryDate().isBefore(po.getOrderDate())) {
            throw new BadRequestException("Confirmed delivery date cannot be before the order date.");
        }
        po.setConfirmedDeliveryDate(request.confirmedDeliveryDate());
        po.setStatus(PurchaseOrderStatus.CONFIRMED);
        auditService.record(TYPE, id, AuditAction.CONFIRMED,
                po.getPoNumber() + " confirmed by supplier for " + request.confirmedDeliveryDate());
        return PurchaseOrderResponse.from(po);
    }

    @Transactional
    public PurchaseOrderResponse receive(Long id, PurchaseOrderReceiveRequest request) {
        PurchaseOrder po = buyerOrder(id);
        requireStatus(po, "receive goods", PurchaseOrderStatus.CONFIRMED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

        StringBuilder summary = new StringBuilder();
        for (PurchaseOrderReceiveRequest.Line line : request.lines()) {
            PurchaseOrderItem item = po.getItems().stream()
                    .filter(i -> i.getId().equals(line.itemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Item " + line.itemId() + " is not on this purchase order."));
            int remaining = item.getOrderedQuantity() - item.getReceivedQuantity();
            if (line.quantity() > remaining) {
                throw new BadRequestException("Cannot receive " + line.quantity() + " of '"
                        + item.getRequisitionItem().getItemName() + "': only " + remaining + " still expected.");
            }
            item.setReceivedQuantity(item.getReceivedQuantity() + line.quantity());
            summary.append(item.getRequisitionItem().getItemName()).append(" x").append(line.quantity()).append("; ");
        }
        boolean complete = po.getItems().stream()
                .allMatch(i -> i.getReceivedQuantity().equals(i.getOrderedQuantity()));
        po.setStatus(complete ? PurchaseOrderStatus.RECEIVED : PurchaseOrderStatus.PARTIALLY_RECEIVED);
        auditService.record(TYPE, id, AuditAction.RECEIVED,
                po.getPoNumber() + " received: " + summary + "status now " + po.getStatus());
        return PurchaseOrderResponse.from(po);
    }

    @Transactional
    public PurchaseOrderResponse close(Long id, PurchaseOrderCloseRequest request) {
        PurchaseOrder po = buyerOrder(id);
        requireStatus(po, "closed", PurchaseOrderStatus.RECEIVED);
        po.setClosureComments(request == null || request.comments() == null ? null : request.comments().trim());
        po.setStatus(PurchaseOrderStatus.CLOSED);
        auditService.record(TYPE, id, AuditAction.CLOSED, po.getPoNumber() + " closed");
        return PurchaseOrderResponse.from(po);
    }

    @Transactional
    public PurchaseOrderResponse cancel(Long id, PurchaseOrderCancelRequest request) {
        PurchaseOrder po = buyerOrder(id);
        requireStatus(po, "cancelled", PurchaseOrderStatus.CREATED, PurchaseOrderStatus.ISSUED,
                PurchaseOrderStatus.CONFIRMED);
        po.setCancelReason(request.reason().trim());
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        po.getRequisition().setStatus(RequisitionStatus.APPROVED);
        auditService.record(TYPE, id, AuditAction.CANCELLED, po.getPoNumber() + " cancelled: " + po.getCancelReason());
        auditService.record("REQUISITION", po.getRequisition().getId(), AuditAction.STATUS_CHANGED,
                po.getRequisition().getRequisitionNumber() + " back to APPROVED after " + po.getPoNumber() + " was cancelled");
        return PurchaseOrderResponse.from(po);
    }

    // ---------------------------------------------------------------- helpers

    private PurchaseOrder find(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Purchase order", id));
    }

    private PurchaseOrder buyerOrder(Long id) {
        requireBuyer(currentUserProvider.requireCurrentUser());
        return find(id);
    }

    private void requireBuyer(User me) {
        if (!BUYERS.contains(me.getRole().getName())) {
            throw new ForbiddenOperationException("Only a procurement officer or admin can manage purchase orders.");
        }
    }

    private void requireStatus(PurchaseOrder po, String action, PurchaseOrderStatus... allowed) {
        for (PurchaseOrderStatus s : allowed) {
            if (po.getStatus() == s) {
                return;
            }
        }
        throw new ConflictException(po.getPoNumber() + " is " + po.getStatus() + " and cannot be " + action + ".");
    }

    private void assertCanView(User me, PurchaseOrder po) {
        String role = me.getRole().getName();
        Requisition r = po.getRequisition();
        boolean allowed = r.getRequester().getId().equals(me.getId())
                || SEES_ALL.contains(role)
                || (SEES_DEPARTMENT.contains(role) && r.getDepartment().getId().equals(me.getDepartment().getId()));
        if (!allowed) {
            throw new ForbiddenOperationException("You are not allowed to view this purchase order.");
        }
    }

    /** Only an active, non-blacklisted, compliant supplier registered for every item category may receive an order. */
    private void checkSupplier(Supplier supplier, Requisition requisition) {
        if (!supplier.isActive() || supplier.getStatus() != SupplierStatus.ACTIVE) {
            throw new ConflictException("Supplier " + supplier.getName() + " is " + supplier.getStatus()
                    + " and cannot receive orders.");
        }
        if (supplier.getComplianceStatus() == ComplianceStatus.NON_COMPLIANT) {
            throw new ConflictException("Supplier " + supplier.getName() + " is NON_COMPLIANT and cannot receive orders.");
        }
        Set<Long> supplied = supplier.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        for (RequisitionItem item : requisition.getItems()) {
            if (!supplied.contains(item.getCategory().getId())) {
                throw new ConflictException("Supplier " + supplier.getName() + " is not registered for category "
                        + item.getCategory().getName() + " (item '" + item.getItemName() + "').");
            }
        }
    }
}
