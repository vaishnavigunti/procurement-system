package com.infosys.procurement_system.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Writes a ReportTable as an .xlsx workbook. */
final class ExcelReportWriter {

    private ExcelReportWriter() {
    }

    static byte[] write(ReportTable table) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Report");

            Font bold = workbook.createFont();
            bold.setBold(true);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(bold);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(bold);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue(table.title());
            titleCell.setCellStyle(titleStyle);
            if (table.subtitle() != null) {
                sheet.createRow(rowIndex++).createCell(0).setCellValue(table.subtitle());
            }
            rowIndex++;

            int columns = table.headers().size();
            int[] widths = new int[columns];
            Row header = sheet.createRow(rowIndex++);
            for (int c = 0; c < columns; c++) {
                Cell cell = header.createCell(c);
                cell.setCellValue(table.headers().get(c));
                cell.setCellStyle(headerStyle);
                widths[c] = table.headers().get(c).length();
            }
            for (List<Object> values : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int c = 0; c < columns; c++) {
                    Object v = c < values.size() ? values.get(c) : null;
                    Cell cell = row.createCell(c);
                    if (v instanceof BigDecimal b) {
                        cell.setCellValue(b.doubleValue());
                    } else if (v instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else {
                        cell.setCellValue(v == null ? "" : v.toString());
                    }
                    widths[c] = Math.max(widths[c], v == null ? 0 : v.toString().length());
                }
            }
            for (int c = 0; c < columns; c++) {
                sheet.setColumnWidth(c, Math.min(60, widths[c] + 3) * 256);
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create the Excel report", e);
        }
    }
}
