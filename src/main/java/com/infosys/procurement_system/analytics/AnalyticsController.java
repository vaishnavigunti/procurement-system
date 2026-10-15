package com.infosys.procurement_system.analytics;

import com.infosys.procurement_system.analytics.AnalyticsModels.ApproverPerformance;
import com.infosys.procurement_system.analytics.AnalyticsModels.CategorySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.DepartmentSpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.MonthlySpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.SupplierSpend;
import com.infosys.procurement_system.analytics.AnalyticsModels.Summary;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService service;

    /** Counts by status, money in flight and active suppliers: the dashboard header. */
    @GetMapping("/summary")
    public Summary summary(@RequestParam(required = false) Long departmentId) {
        return service.summary(departmentId);
    }

    @GetMapping("/spend/by-department")
    public List<DepartmentSpend> byDepartment(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.spendByDepartment(departmentId, from, to);
    }

    @GetMapping("/spend/by-category")
    public List<CategorySpend> byCategory(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.spendByCategory(departmentId, from, to);
    }

    @GetMapping("/spend/by-supplier")
    public List<SupplierSpend> bySupplier(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.spendBySupplier(departmentId, from, to);
    }

    @GetMapping("/spend/monthly")
    public List<MonthlySpend> monthly(@RequestParam(required = false) Long departmentId,
                                      @RequestParam(required = false) Integer year) {
        return service.monthlySpend(departmentId, year);
    }

    @GetMapping("/approvals/performance")
    public List<ApproverPerformance> approvals(@RequestParam(required = false) Long departmentId) {
        return service.approverPerformance(departmentId);
    }
}
