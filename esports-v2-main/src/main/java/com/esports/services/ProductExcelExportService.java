package com.esports.services;

import com.esports.models.Product;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ProductExcelExportService {

    private static final String EXPORT_FOLDER = "exports";

    public File exportProducts(List<Product> products) throws Exception {
        if (products == null || products.isEmpty()) {
            throw new IllegalArgumentException("No products to export.");
        }

        File folder = new File(EXPORT_FOLDER);

        if (!folder.exists()) {
            boolean created = folder.mkdirs();

            if (!created) {
                throw new IllegalStateException("Could not create exports folder.");
            }
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        File file = new File(folder, "products_export_" + timestamp + ".xlsx");

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream outputStream = new FileOutputStream(file)) {

            Sheet sheet = workbook.createSheet("Products");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ID");
            header.createCell(1).setCellValue("Name");
            header.createCell(2).setCellValue("Category");
            header.createCell(3).setCellValue("Description");
            header.createCell(4).setCellValue("Price TND");
            header.createCell(5).setCellValue("Stock");
            header.createCell(6).setCellValue("Orders");
            header.createCell(7).setCellValue("Forecast Period");
            header.createCell(8).setCellValue("Recommended Reorder Qty");
            header.createCell(9).setCellValue("Stock Status");
            header.createCell(10).setCellValue("Image");

            int rowIndex = 1;

            for (Product product : products) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(product.getId());
                row.createCell(1).setCellValue(safe(product.getName()));
                row.createCell(2).setCellValue(safe(product.getCategory()));
                row.createCell(3).setCellValue(safe(product.getDescription()));
                row.createCell(4).setCellValue(product.getPrice());
                row.createCell(5).setCellValue(product.getStock());
                row.createCell(6).setCellValue(product.getOrdersCount());
                row.createCell(7).setCellValue(product.getForecastDays());
                row.createCell(8).setCellValue(product.getRecommendedReorderQty());
                row.createCell(9).setCellValue(safe(product.getMlRiskLevel()));
                row.createCell(10).setCellValue(safe(product.getImage()));
            }

            for (int i = 0; i <= 10; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }

        return file;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}