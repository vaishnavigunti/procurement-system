package com.infosys.procurement_system.report;

import com.infosys.procurement_system.analytics.AnalyticsModels.ApproverPerformance;
import com.infosys.procurement_system.analytics.AnalyticsModels.CategorySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.DepartmentSpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.MonthlySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.SupplierSpend;
import com.infosys.procurement_system.analytics.AnalyticsService;
import com.infosys.procurement_system.purchaseorder.PurchaseOrderResponse;
import com.infosys.procurement_system.purchaseorder.PurchaseOrderService;
import com.infosys.procurement_system.requisition.RequisitionResponse;
import com.infosys.procurement_system.requisition.RequisitionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * Builds report tables by reusing the analytics and list services, so access rules and numbers
 * always match what the dashboard shows. Rendering to PDF or Excel is done by the writers.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_ROWS = 2000;

    private final AnalyticsService analyticsService;
    private final RequisitionService requisitionService;
    private final PurchaseOrderService purchaseOrderService;

    public byte[] render(ReportType type, ReportFormat format, Long departmentId, LocalDate from, LocalDate to) {
        ReportTable table = build(type, departmentId, from, to);
        return format == ReportFormat.PDF ? PdfReportWriter.write(table) : ExcelReportWriter.write(table);
    }

    public ReportTable build(ReportType type, Long departmentId, LocalDate from, LocalDate to) {
        String subtitle = "Generated " + LocalDateTime.now(ZoneId.systemDefault()).format(STAMP)
                + (from != null || to != null ? "  |  Period: " + (from == null ? "start" : from) + " to " + (to == null ? "today" : to) : "")
                + "  |  Cancelled purchase orders are excluded from spend";
        return switch (type) {
            case SPEND_BY_DEPARTMENT -> {
                List<List<Object>> rows = new ArrayList<>();
                for (DepartmentSpend d : analyticsService.spendByDepartment(departmentId, from, to)) {
                    rows.add(List.of(d.department(), d.annualBudget(), d.spent(), d.utilisationPercent()));
                }
                yield new ReportTable("Spend by department", subtitle,
                        List.of("Department", "Annual budget", "Spent", "Budget used %"), rows);
            }
            case SPEND_BY_CATEGORY -> {
                List<List<Object>> rows = new ArrayList<>();
                for (CategorySpend c : analyticsService.spendByCategory(departmentId, from, to)) {
                    rows.add(List.of(c.category(), c.spent()));
                }
                yield new ReportTable("Spend by category", subtitle, List.of("Category", "Spent"), rows);
            }
            case SPEND_BY_SUPPLIER -> {
                List<List<Object>> rows = new ArrayList<>();
                for (SupplierSpend s : analyticsService.spendBySupplier(departmentId, from, to)) {
                    List<Object> row = new ArrayList<>();
                    row.add(s.supplier());
                    row.add(s.performanceRating() == null ? "" : s.performanceRating());
                    row.add(s.complianceStatus());
                    row.add(s.purchaseOrders());
                    row.add(s.spent());
                    rows.add(row);
                }
                yield new ReportTable("Spend by supplier", subtitle,
                        List.of("Supplier", "Rating", "Compliance", "Purchase orders", "Spent"), rows);
            }
            case MONTHLY_SPEND -> {
                List<List<Object>> rows = new ArrayList<>();
                Integer year = from != null ? from.getYear() : null;
                for (MonthlySpend m : analyticsService.monthlySpend(departmentId, year)) {
                    rows.add(List.of(m.month(), m.purchaseOrders(), m.spent()));
                }
                yield new ReportTable("Monthly spend" + (year == null ? "" : " " + year), subtitle,
                        List.of("Month", "Purchase orders", "Spent"), rows);
            }
            case APPROVER_PERFORMANCE -> {
                List<List<Object>> rows = new ArrayList<>();
                for (ApproverPerformance a : analyticsService.approverPerformance(departmentId)) {
                    List<Object> row = new ArrayList<>();
                    row.add(a.role());
                    row.add(a.decisions());
                    row.add(a.approved());
                    row.add(a.rejected());
                    row.add(a.returned());
                    row.add(a.averageHoursFromSubmission() == null ? "" : a.averageHoursFromSubmission());
                    rows.add(row);
                }
                yield new ReportTable("Approver performance", subtitle,
                        List.of("Role", "Decisions", "Approved", "Rejected", "Returned", "Avg hours (approx.)"), rows);
            }
            case REQUISITIONS -> {
                List<List<Object>> rows = new ArrayList<>();
                var page = requisitionService.list(null, null,
                        PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "id")));
                for (RequisitionResponse r : page.content()) {
                    List<Object> row = new ArrayList<>();
                    row.add(r.requisitionNumber());
                    row.add(r.title());
                    row.add(r.requesterName());
                    row.add(r.departmentName());
                    row.add(r.priority());
                    row.add(r.status());
                    row.add(r.totalAmount());
                    row.add(r.submittedAt() == null ? "" : r.submittedAt().toString());
                    rows.add(row);
                }
                yield new ReportTable("Requisitions", subtitle,
                        List.of("Number", "Title", "Requester", "Department", "Priority", "Status", "Total", "Submitted at"), rows);
            }
            case PURCHASE_ORDERS -> {
                List<List<Object>> rows = new ArrayList<>();
                var page = purchaseOrderService.list(null, null,
                        PageRequest.of(0, MAX_ROWS, Sort.by(Sort.Direction.DESC, "id")));
                for (PurchaseOrderResponse p : page.content()) {
                    rows.add(List.of(p.poNumber(), p.requisitionNumber(), p.supplierName(), p.status(),
                            String.valueOf(p.orderDate()), p.totalAmount(), p.fulfillmentPercent()));
                }
                yield new ReportTable("Purchase orders", subtitle,
                        List.of("PO number", "Requisition", "Supplier", "Status", "Order date", "Total", "Received %"), rows);
            }
        };
    }
}
