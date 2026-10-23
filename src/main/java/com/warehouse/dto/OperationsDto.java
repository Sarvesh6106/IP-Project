package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class OperationsDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockDto {
        private Long id;
        private Long branchId;
        private String branchName;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private Integer minimumStockThreshold;
        private boolean lowStock;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockUpdateDto {
        private Long branchId;
        private Long productId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferCreateDto {
        private Long sourceBranchId;
        private Long destinationBranchId;
        private Long productId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferResponseDto {
        private Long id;
        private Long sourceBranchId;
        private String sourceBranchName;
        private Long destinationBranchId;
        private String destinationBranchName;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer quantity;
        private String status;
        private Long requestedById;
        private String requestedByName;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockMovementDto {
        private Long id;
        private Long branchId;
        private String branchName;
        private Long productId;
        private String productName;
        private String productSku;
        private String movementType;
        private Integer quantityChange;
        private Integer previousQuantity;
        private Integer newQuantity;
        private Long referenceId;
        private String performedBy;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseCreateDto {
        private Long branchId;
        private Long productId;
        private Integer requestedQuantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseResponseDto {
        private Long id;
        private Long branchId;
        private String branchName;
        private Long productId;
        private String productName;
        private String productSku;
        private Integer requestedQuantity;
        private String status;
        private LocalDateTime createdAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusActionDto {
        private String action;
    }
}
