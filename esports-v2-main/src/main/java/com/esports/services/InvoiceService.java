package com.esports.services;

import com.esports.models.Order;
import com.esports.models.OrderItem;
import com.lowagie.text.Cell;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class InvoiceService {

    private static final String INVOICE_FOLDER = "invoices";

    public File generateInvoice(Order order, List<OrderItem> items) throws Exception {
        if (order == null) {
            throw new IllegalArgumentException("Order cannot be null.");
        }

        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order items cannot be empty.");
        }

        File folder = new File(INVOICE_FOLDER);

        if (!folder.exists()) {
            boolean created = folder.mkdirs();

            if (!created) {
                throw new IllegalStateException("Could not create invoices folder.");
            }
        }

        String reference = order.getReference();

        if (reference == null || reference.trim().isEmpty()) {
            reference = "unknown_order_" + order.getId();
        }

        String fileName = "invoice_" + reference + ".pdf";
        File pdfFile = new File(folder, fileName);

        Document document = new Document(PageSize.A4);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

        document.open();

        addHeader(document, order);
        addCustomerInfo(document, order);
        addItemsTable(document, items);
        addTotal(document, order);
        addFooter(document);

        document.close();

        return pdfFile;
    }

    private void addHeader(Document document, Order order) throws DocumentException {
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.BLACK);
        Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 12, Color.DARK_GRAY);

        Paragraph title = new Paragraph("E-Sports Store - Invoice", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph reference = new Paragraph("Invoice Reference: " + safe(order.getReference()), subFont);
        reference.setAlignment(Element.ALIGN_CENTER);
        reference.setSpacingAfter(20);
        document.add(reference);
    }

    private void addCustomerInfo(Document document, Order order) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11, Color.BLACK);

        document.add(new Paragraph("Customer Information", sectionFont));
        document.add(new Paragraph("Name: " + safe(order.getCustomerFirstName()) + " " + safe(order.getCustomerLastName()), normalFont));
        document.add(new Paragraph("Email: " + safe(order.getCustomerEmail()), normalFont));
        document.add(new Paragraph("Phone: " + safe(order.getCustomerPhone()), normalFont));
        document.add(new Paragraph("Payment Method: " + safe(order.getPaymentMethod()), normalFont));
        document.add(new Paragraph("Payment Status: " + safe(order.getPaymentStatus()), normalFont));
        document.add(new Paragraph("Order Status: " + safe(order.getStatus()), normalFont));

        if (order.getCreatedAt() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            document.add(new Paragraph("Date: " + order.getCreatedAt().format(formatter), normalFont));
        }

        document.add(new Paragraph(" "));
    }

    private void addItemsTable(Document document, List<OrderItem> items) throws DocumentException {
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.WHITE);
        Font cellFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        document.add(new Paragraph("Order Details", sectionFont));
        document.add(new Paragraph(" "));

        Table table = new Table(5);
        table.setWidth(100);
        table.setPadding(5);
        table.setSpacing(1);

        addHeaderCell(table, "Product", headerFont);
        addHeaderCell(table, "Quantity", headerFont);
        addHeaderCell(table, "Unit Price", headerFont);
        addHeaderCell(table, "Subtotal", headerFont);
        addHeaderCell(table, "Product ID", headerFont);

        for (OrderItem item : items) {
            table.addCell(new Phrase(safe(item.getProductName()), cellFont));
            table.addCell(new Phrase(String.valueOf(item.getQuantity()), cellFont));
            table.addCell(new Phrase(String.format("%.2f", item.getUnitPrice()), cellFont));
            table.addCell(new Phrase(String.format("%.2f", item.getSubtotal()), cellFont));
            table.addCell(new Phrase(String.valueOf(item.getProductId()), cellFont));
        }

        document.add(table);
    }

    private void addHeaderCell(Table table, String text, Font font) {
        Cell cell = new Cell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(40, 40, 80));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }

    private void addTotal(Document document, Order order) throws DocumentException {
        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.BLACK);

        Paragraph total = new Paragraph("Total Amount: " + String.format("%.2f", order.getTotalAmount()), totalFont);
        total.setAlignment(Element.ALIGN_RIGHT);
        total.setSpacingBefore(20);

        document.add(total);
    }

    private void addFooter(Document document) throws DocumentException {
        Font footerFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10, Color.GRAY);

        Paragraph footer = new Paragraph("Thank you for your order.", footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        footer.setSpacingBefore(30);

        document.add(footer);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}