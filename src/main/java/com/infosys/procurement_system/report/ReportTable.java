package com.infosys.procurement_system.report;

import java.util.List;

/** A report as a simple table: headers plus rows. Cells may be numbers (kept numeric in Excel) or text. */
public record ReportTable(String title, String subtitle, List<String> headers, List<List<Object>> rows) {
}
