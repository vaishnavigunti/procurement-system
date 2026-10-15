package com.infosys.procurement_system.analytics;

import com.infosys.procurement_system.analytics.AnalyticsModels.ApproverPerformance;
import com.infosys.procurement_system.analytics.AnalyticsModels.CategorySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.DepartmentSpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.MonthlySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.SupplierSpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.Summary;
import com.infosys.procurement_system.approval.Approval;
import com.infosys.procurement_system.approval.ApprovalStatus;
import com.infosys.procurement_system.common.CurrentUserProvider;
import com.infosys.procurement_system.common.exception.ForbiddenOperationException;
import com.infosys.procurement_system.purchaseorder.PurchaseOrderStatus;
import com.infosys.procurement_system.requisition.RequisitionStatus;
import com.infosys.procurement_system.supplier.SupplierStatus;
import com.infosys.procurement_system.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only procurement analytics. Purchase-order spend excludes cancelled orders.
 * Managers and department heads only see their own department; other allowed roles see everything.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final Set<String> ALLOWED = Set.of(
            "ADMIN", "FINANCE_HEAD", "PROCUREMENT_OFFICER", "MANAGER", "DEPARTMENT_HEAD");
    private static final Set<String> DEPARTMENT_SCOPED = Set.of("MANAGER", "DEPARTMENT_HEAD");

    private final EntityManager entityManager;
    private final CurrentUserProvider currentUserProvider;

    public Summary summary(Long departmentId) {
        Long dept = scope(departmentId);

        Map<String, Long> requisitions = zeroed(RequisitionStatus.values());
        Query rq = entityManager.createQuery("select r.status, count(r) from Requisition r"
                + (dept != null ? " where r.department.id = :dept" : "") + " group by r.status");
        bind(rq, dept);
        for (Object row : rq.getResultList()) {
            Object[] c = (Object[]) row;
            requisitions.put(c[0].toString(), ((Number) c[1]).longValue());
        }

        Map<String, Long> orders = zeroed(PurchaseOrderStatus.values());
        Query pq = entityManager.createQuery("select po.status, count(po) from PurchaseOrder po join po.requisition r"
                + (dept != null ? " where r.department.id = :dept" : "") + " group by po.status");
        bind(pq, dept);
        for (Object row : pq.getResultList()) {
            Object[] c = (Object[]) row;
            orders.put(c[0].toString(), ((Number) c[1]).longValue());
        }

        Query sq = entityManager.createQuery("select count(s) from Supplier s where s.status = :st");
        sq.setParameter("st", SupplierStatus.ACTIVE);
        long activeSuppliers = ((Number) sq.getSingleResult()).longValue();

        Query vq = entityManager.createQuery("select sum(po.totalAmount) from PurchaseOrder po join po.requisition r"
                + " where po.status <> :cancelled" + (dept != null ? " and r.department.id = :dept" : ""));
        vq.setParameter("cancelled", PurchaseOrderStatus.CANCELLED);
        bind(vq, dept);

        Query aq = entityManager.createQuery("select sum(r.totalAmount) from Requisition r where r.status = :st"
                + (dept != null ? " and r.department.id = :dept" : ""));
        aq.setParameter("st", RequisitionStatus.APPROVED);
        bind(aq, dept);

        return new Summary(requisitions, orders, requisitions.get(RequisitionStatus.PENDING_APPROVAL.name()),
                activeSuppliers, money(vq.getSingleResult()), money(aq.getSingleResult()));
    }

    public List<DepartmentSpend> spendByDepartment(Long departmentId, LocalDate from, LocalDate to) {
        Long dept = scope(departmentId);
        Query q = poQuery("select d.id, d.name, d.annualBudget, sum(po.totalAmount)"
                        + " from PurchaseOrder po join po.requisition r join r.department d",
                " group by d.id, d.name, d.annualBudget", dept, from, to);
        List<DepartmentSpend> result = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] c = (Object[]) row;
            BigDecimal budget = money(c[2]);
            BigDecimal spent = money(c[3]);
            BigDecimal percent = budget.signum() == 0 ? BigDecimal.ZERO
                    : spent.multiply(BigDecimal.valueOf(100)).divide(budget, 1, RoundingMode.HALF_UP);
            result.add(new DepartmentSpend(((Number) c[0]).longValue(), (String) c[1], budget, spent, percent));
        }
        result.sort(Comparator.comparing(DepartmentSpend::spent).reversed());
        return result;
    }

    public List<CategorySpend> spendByCategory(Long departmentId, LocalDate from, LocalDate to) {
        Long dept = scope(departmentId);
        Query q = poQuery("select c.id, c.name, sum(pi.orderedQuantity * pi.unitPrice)"
                        + " from PurchaseOrderItem pi join pi.purchaseOrder po join po.requisition r"
                        + " join pi.requisitionItem ri join ri.category c",
                " group by c.id, c.name", dept, from, to);
        List<CategorySpend> result = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] c = (Object[]) row;
            result.add(new CategorySpend(((Number) c[0]).longValue(), (String) c[1], money(c[2])));
        }
        result.sort(Comparator.comparing(CategorySpend::spent).reversed());
        return result;
    }

    public List<SupplierSpend> spendBySupplier(Long departmentId, LocalDate from, LocalDate to) {
        Long dept = scope(departmentId);
        Query q = poQuery("select s.id, s.name, s.performanceRating, s.complianceStatus, count(po), sum(po.totalAmount)"
                        + " from PurchaseOrder po join po.requisition r join po.supplier s",
                " group by s.id, s.name, s.performanceRating, s.complianceStatus", dept, from, to);
        List<SupplierSpend> result = new ArrayList<>();
        for (Object row : q.getResultList()) {
            Object[] c = (Object[]) row;
            result.add(new SupplierSpend(((Number) c[0]).longValue(), (String) c[1],
                    c[2] == null ? null : money(c[2]), String.valueOf(c[3]),
                    ((Number) c[4]).longValue(), money(c[5])));
        }
        result.sort(Comparator.comparing(SupplierSpend::spent).reversed());
        return result;
    }

    public List<MonthlySpend> monthlySpend(Long departmentId, Integer year) {
        Long dept = scope(departmentId);
        int y = year == null ? LocalDate.now().getYear() : year;
        Query q = poQuery("select po.orderDate, po.totalAmount from PurchaseOrder po join po.requisition r",
                "", dept, LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31));

        Map<YearMonth, BigDecimal> spent = new TreeMap<>();
        Map<YearMonth, Long> counts = new TreeMap<>();
        for (Month m : Month.values()) {
            spent.put(YearMonth.of(y, m), BigDecimal.ZERO);
            counts.put(YearMonth.of(y, m), 0L);
        }
        for (Object row : q.getResultList()) {
            Object[] c = (Object[]) row;
            YearMonth ym = YearMonth.from((LocalDate) c[0]);
            spent.merge(ym, money(c[1]), BigDecimal::add);
            counts.merge(ym, 1L, Long::sum);
        }
        List<MonthlySpend> result = new ArrayList<>();
        spent.forEach((ym, amount) -> result.add(new MonthlySpend(ym.toString(), counts.get(ym), amount)));
        return result;
    }

    /** Decisions per approver role. Average hours are measured from the requisition's latest submission (approximate). */
    public List<ApproverPerformance> approverPerformance(Long departmentId) {
        Long dept = scope(departmentId);
        Query q = entityManager.createQuery("select a from Approval a join fetch a.workflowStep ws"
                + " join fetch ws.approverRole join fetch a.requisition r"
                + " where a.decidedAt is not null and r.submittedAt is not null"
                + (dept != null ? " and r.department.id = :dept" : ""));
        bind(q, dept);

        Map<String, long[]> counts = new LinkedHashMap<>();
        Map<String, List<Double>> hours = new LinkedHashMap<>();
        for (Object row : q.getResultList()) {
            Approval a = (Approval) row;
            String role = a.getWorkflowStep().getApproverRole().getName();
            long[] c = counts.computeIfAbsent(role, k -> new long[4]);
            c[0]++;
            if (a.getStatus() == ApprovalStatus.APPROVED) {
                c[1]++;
            } else if (a.getStatus() == ApprovalStatus.REJECTED) {
                c[2]++;
            } else if (a.getStatus() == ApprovalStatus.RETURNED) {
                c[3]++;
            }
            double h = Duration.between(a.getRequisition().getSubmittedAt(), a.getDecidedAt()).toMinutes() / 60.0;
            if (h >= 0) {
                hours.computeIfAbsent(role, k -> new ArrayList<>()).add(h);
            }
        }
        List<ApproverPerformance> result = new ArrayList<>();
        counts.forEach((role, c) -> {
            List<Double> list = hours.getOrDefault(role, List.of());
            Double avg = list.isEmpty() ? null
                    : Math.round(list.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 10) / 10.0;
            result.add(new ApproverPerformance(role, c[0], c[1], c[2], c[3], avg));
        });
        return result;
    }

    // ---------------------------------------------------------------- helpers

    /** Checks the caller may see analytics and returns the department to filter by (null = all). */
    private Long scope(Long requestedDepartmentId) {
        User me = currentUserProvider.requireCurrentUser();
        String role = me.getRole().getName();
        if (!ALLOWED.contains(role)) {
            throw new ForbiddenOperationException("Analytics are available to managers, heads, procurement and finance roles.");
        }
        return DEPARTMENT_SCOPED.contains(role) ? me.getDepartment().getId() : requestedDepartmentId;
    }

    /** Builds a purchase-order query (non-cancelled only) with optional department and date filters. */
    private Query poQuery(String select, String groupBy, Long dept, LocalDate from, LocalDate to) {
        StringBuilder jpql = new StringBuilder(select).append(" where po.status <> :cancelled");
        if (dept != null) {
            jpql.append(" and r.department.id = :dept");
        }
        if (from != null) {
            jpql.append(" and po.orderDate >= :from");
        }
        if (to != null) {
            jpql.append(" and po.orderDate <= :to");
        }
        jpql.append(groupBy);
        Query q = entityManager.createQuery(jpql.toString());
        q.setParameter("cancelled", PurchaseOrderStatus.CANCELLED);
        bind(q, dept);
        if (from != null) {
            q.setParameter("from", from);
        }
        if (to != null) {
            q.setParameter("to", to);
        }
        return q;
    }

    private void bind(Query q, Long dept) {
        if (dept != null) {
            q.setParameter("dept", dept);
        }
    }

    private Map<String, Long> zeroed(Enum<?>[] values) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (Enum<?> v : values) {
            map.put(v.name(), 0L);
        }
        return map;
    }

    private BigDecimal money(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal b ? b : BigDecimal.valueOf(((Number) value).doubleValue());
    }
}
