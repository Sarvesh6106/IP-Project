package com.warehouse.service;

import com.warehouse.dto.DashboardDto;
import com.warehouse.dto.OperationsDto;
import com.warehouse.entity.PurchaseRequest;
import com.warehouse.entity.TransferRequest;
import com.warehouse.repository.BranchRepository;
import com.warehouse.repository.ProductRepository;
import com.warehouse.repository.PurchaseRequestRepository;
import com.warehouse.repository.StockRepository;
import com.warehouse.repository.TransferRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final OperationsService operationsService;

    public DashboardDto.DashboardSummaryDto getSummary() {
        long totalBranches = branchRepository.count();
        long totalProducts = productRepository.count();
        long lowStockCount = stockRepository.countLowStockItems();
        long pendingTransfers = transferRequestRepository.countByStatus(TransferRequest.Status.PENDING);
        long pendingPurchases = purchaseRequestRepository.countByStatus(PurchaseRequest.Status.PENDING);

        List<OperationsDto.StockDto> lowStockList = stockRepository.findLowStockItems().stream()
                .map(operationsService::mapToStockDto)
                .collect(Collectors.toList());

        return new DashboardDto.DashboardSummaryDto(
                totalBranches,
                totalProducts,
                lowStockCount,
                pendingTransfers,
                pendingPurchases,
                lowStockList
        );
    }
}
