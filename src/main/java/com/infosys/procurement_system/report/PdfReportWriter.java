package com.infosys.procurement_system.report;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** Writes a ReportTable as a landscape A4 PDF. */
final class PdfReportWriter {

    private PdfReportWriter() {
    }

    static byte[] write(ReportTable table) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4.rotate(), 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            document.add(new Paragraph(table.title(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            if (table.subtitle() != null) {
                document.add(new Paragraph(table.subtitle(), FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY)));
            }
            document.add(new Paragraph(" "));

            Font headFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            int columns = table.headers().size();
            PdfPTable pdfTable = new PdfPTable(columns);
            pdfTable.setWidthPercentage(100);
            pdfTable.setHeaderRows(1);

            for (String h : table.headers()) {
                PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
                cell.setBackgroundColor(new Color(220, 220, 220));
                cell.setPadding(5);
                pdfTable.addCell(cell);
            }
            for (List<Object> values : table.rows()) {
                for (int c = 0; c < columns; c++) {
                    Object v = c < values.size() ? values.get(c) : null;
                    PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v.toString(), bodyFont));
                    cell.setPadding(4);
                    if (v instanceof Number) {
                        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    pdfTable.addCell(cell);
                }
            }
            if (table.rows().isEmpty()) {
                document.add(new Paragraph("No data for this report.", bodyFont));
            } else {
                document.add(pdfTable);
            }
        } catch (DocumentException e) {
            throw new IllegalStateException("Could not create the PDF report", e);
        } finally {
            document.close();
        }
        return out.toByteArray();
    }
}
