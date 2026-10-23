package com.warehouse.service;

import com.warehouse.dto.OperationsDto;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    public byte[] createInventoryWorkbook(List<OperationsDto.StockDto> stocks) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Inventory");
            String[] headers = {
                    "Product ID", "SKU", "Product Name", "Branch ID",
                    "Branch", "Quantity", "Minimum Stock Threshold", "Stock Status"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            int rowIndex = 1;
            for (OperationsDto.StockDto stock : stocks) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(stock.getProductId());
                row.createCell(1).setCellValue(safe(stock.getProductSku()));
                row.createCell(2).setCellValue(safe(stock.getProductName()));
                row.createCell(3).setCellValue(stock.getBranchId());
                row.createCell(4).setCellValue(safe(stock.getBranchName()));
                row.createCell(5).setCellValue(stock.getQuantity() == null ? 0 : stock.getQuantity());
                row.createCell(6).setCellValue(stock.getMinimumStockThreshold() == null ? 0 : stock.getMinimumStockThreshold());
                row.createCell(7).setCellValue(stock.isLowStock() ? "LOW STOCK" : "HEALTHY");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to generate inventory Excel file", ex);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
