package com.warehouse.controller;

import com.warehouse.dto.OperationsDto;
import com.warehouse.service.ExcelExportService;
import com.warehouse.service.OperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
public class InventoryExportController {

    private final OperationsService operationsService;
    private final ExcelExportService excelExportService;

    @GetMapping("/inventory")
    public ResponseEntity<byte[]> exportInventory(
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        List<OperationsDto.StockDto> stocks =
                operationsService.getStocksForExport(branchId, authentication.getName());
        byte[] file = excelExportService.createInventoryWorkbook(stocks);
        String filename = "warehouse_inventory_" + LocalDate.now() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .contentLength(file.length)
                .body(file);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
