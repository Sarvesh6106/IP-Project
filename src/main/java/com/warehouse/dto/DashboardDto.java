package com.warehouse.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

public class DashboardDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummaryDto {
        private long totalBranches;
        private long totalProducts;
        private long lowStockCount;
        private long pendingTransfersCount;
        private long pendingPurchasesCount;
        private List<OperationsDto.StockDto> lowStockItems;
    }
}
