package com.warehouse.controller;

import com.warehouse.dto.OperationsDto;
import com.warehouse.service.OperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/operations")
@RequiredArgsConstructor
public class OperationsController {

    private final OperationsService operationsService;

    @GetMapping("/stocks")
    public ResponseEntity<List<OperationsDto.StockDto>> getStocks(@RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(operationsService.getStocks(branchId));
    }

    @PostMapping("/stocks")
    public ResponseEntity<OperationsDto.StockDto> updateStock(
            @RequestBody OperationsDto.StockUpdateDto dto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operationsService.updateStock(dto, authentication.getName()));
    }

    @GetMapping("/stock-movements")
    public ResponseEntity<List<OperationsDto.StockMovementDto>> getStockMovements(
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ResponseEntity.ok(operationsService.getStockMovements(branchId, authentication.getName()));
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<OperationsDto.TransferResponseDto>> getTransfers(
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ResponseEntity.ok(operationsService.getTransferRequests(branchId, authentication.getName()));
    }

    @PostMapping("/transfers")
    public ResponseEntity<OperationsDto.TransferResponseDto> createTransfer(
            @RequestBody OperationsDto.TransferCreateDto dto,
            Authentication authentication
    ) {
        String username = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(operationsService.createTransferRequest(dto, username));
    }

    @PutMapping("/transfers/{id}/status")
    public ResponseEntity<OperationsDto.TransferResponseDto> updateTransferStatus(
            @PathVariable Long id,
            @RequestBody OperationsDto.StatusActionDto actionDto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operationsService.handleTransferStatus(
                id, actionDto.getAction(), authentication.getName()));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<OperationsDto.PurchaseResponseDto>> getPurchases(
            @RequestParam(required = false) Long branchId,
            Authentication authentication) {
        return ResponseEntity.ok(operationsService.getPurchaseRequests(branchId, authentication.getName()));
    }

    @PostMapping("/purchases")
    public ResponseEntity<OperationsDto.PurchaseResponseDto> createPurchase(
            @RequestBody OperationsDto.PurchaseCreateDto dto,
            Authentication authentication) {
        return ResponseEntity.ok(operationsService.createPurchaseRequest(dto, authentication.getName()));
    }

    @PutMapping("/purchases/{id}/status")
    public ResponseEntity<OperationsDto.PurchaseResponseDto> updatePurchaseStatus(
            @PathVariable Long id,
            @RequestBody OperationsDto.StatusActionDto actionDto,
            Authentication authentication
    ) {
        return ResponseEntity.ok(operationsService.handlePurchaseStatus(
                id, actionDto.getAction(), authentication.getName()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleException(RuntimeException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
