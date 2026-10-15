package com.infosys.procurement_system.report;

import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Downloadable reports. Types: SPEND_BY_DEPARTMENT, SPEND_BY_CATEGORY, SPEND_BY_SUPPLIER, MONTHLY_SPEND, APPROVER_PERFORMANCE, REQUISITIONS, PURCHASE_ORDERS. */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{type}")
    public ResponseEntity<byte[]> download(
            @PathVariable ReportType type,
            @RequestParam(defaultValue = "PDF") ReportFormat format,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] content = reportService.render(type, format, departmentId, from, to);
        String filename = type.name().toLowerCase().replace('_', '-') + "-" + LocalDate.now() + "." + format.extension();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.parseMediaType(format.mediaType()))
                .body(content);
    }
}
