
package com.warehouse.service;

import com.warehouse.dto.OperationsDto;
import com.warehouse.entity.Branch;
import com.warehouse.entity.Product;
import com.warehouse.entity.PurchaseRequest;
import com.warehouse.entity.Stock;
import com.warehouse.entity.TransferRequest;
import com.warehouse.entity.User;
import com.warehouse.repository.BranchRepository;
import com.warehouse.repository.ProductRepository;
import com.warehouse.repository.PurchaseRequestRepository;
import com.warehouse.repository.StockRepository;
import com.warehouse.repository.TransferRequestRepository;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OperationsService {

    private final StockRepository stockRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final PurchaseRequestRepository purchaseRequestRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    public List<OperationsDto.StockDto> getStocks(Long branchId) {
        List<Stock> stocks;

        if (branchId != null) {
            stocks = stockRepository.findByBranchId(branchId);
        } else {
            stocks = stockRepository.findAll();
        }

        return stocks.stream()
                .map(this::mapToStockDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OperationsDto.StockDto updateStock(OperationsDto.StockUpdateDto dto, String username) {
        if (dto == null || dto.getBranchId() == null || dto.getProductId() == null) {
            throw new RuntimeException("Branch and product are required");
        }

        requireBranchAccess(username, dto.getBranchId());

        if (dto.getQuantity() == null || dto.getQuantity() < 0) {
            throw new RuntimeException("Stock quantity cannot be negative");
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new RuntimeException(
                        "Branch not found: " + dto.getBranchId()));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found: " + dto.getProductId()));

        Stock stock = stockRepository.findByBranchIdAndProductId(
                        dto.getBranchId(), dto.getProductId())
                .orElse(null);
        Integer previousQuantity = stock == null ? null : stock.getQuantity();

        if (stock == null) {
            stock = new Stock(null, branch, product, 0);
        }

        stock.setQuantity(dto.getQuantity());

        Stock saved = stockRepository.save(stock);
        int previous = previousQuantity == null ? 0 : previousQuantity;
        stockMovementService.recordMovement(
                saved.getBranch(),
                saved.getProduct(),
                "STOCK_ADJUSTMENT",
                saved.getQuantity() - previous,
                previous,
                saved.getQuantity(),
                null,
                username
        );
        createAutoPurchaseRequest(saved, previousQuantity);

        return mapToStockDto(saved);
    }

    private void createAutoPurchaseRequest(Stock stock, Integer previousQuantity) {
        Integer threshold = stock.getProduct().getMinimumStockThreshold();

        if (threshold == null || threshold <= 0 || stock.getQuantity() > threshold) {
            return;
        }

        if (previousQuantity != null && previousQuantity <= threshold) {
            return;
        }

        int requestedQuantity = Math.max(1, threshold - stock.getQuantity());

        PurchaseRequest request = new PurchaseRequest(
                null,
                stock.getBranch(),
                stock.getProduct(),
                requestedQuantity,
                PurchaseRequest.Status.PENDING,
                LocalDateTime.now()
        );

        purchaseRequestRepository.save(request);
    }

    public List<OperationsDto.TransferResponseDto> getTransferRequests(Long branchId, String username) {
        User user = getAuthenticatedUser(username);
        List<TransferRequest> list;

        if (user.getRole() == User.Role.ADMIN) {
            list = branchId != null
                    ? transferRequestRepository.findByBranchWithDetails(branchId)
                    : transferRequestRepository.findAllWithDetails();
        } else if (user.getAssignedBranch() == null) {
            list = List.of();
        } else {
            list = transferRequestRepository.findByBranchWithDetails(user.getAssignedBranch().getId());
        }

        return list.stream()
                .map(this::mapToTransferResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OperationsDto.TransferResponseDto createTransferRequest(
            OperationsDto.TransferCreateDto dto, String username) {
        if (dto == null || dto.getSourceBranchId() == null || dto.getProductId() == null) {
            throw new RuntimeException("Source branch and product are required");
        }

        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new RuntimeException("Transfer quantity must be greater than zero");
        }

        User user = getAuthenticatedUser(username);
        Long destinationBranchId;

        if (user.getRole() == User.Role.ADMIN) {
            if (dto.getDestinationBranchId() == null) {
                throw new RuntimeException("Destination branch is required");
            }
            destinationBranchId = dto.getDestinationBranchId();
        } else {
            if (user.getAssignedBranch() == null) {
                throw new RuntimeException("Your account is not assigned to a branch");
            }
            destinationBranchId = user.getAssignedBranch().getId();
        }

        if (dto.getSourceBranchId().equals(destinationBranchId)) {
            throw new RuntimeException("Source and destination branch cannot be the same");
        }

        Branch source = branchRepository.findById(dto.getSourceBranchId())
                .orElseThrow(() -> new RuntimeException(
                        "Source branch not found: " + dto.getSourceBranchId()));

        Branch destination = branchRepository.findById(destinationBranchId)
                .orElseThrow(() -> new RuntimeException(
                        "Destination branch not found: " + destinationBranchId));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException(
                        "Product not found: " + dto.getProductId()));

        TransferRequest request = new TransferRequest(
                null,
                source,
                destination,
                product,
                dto.getQuantity(),
                TransferRequest.Status.PENDING,
                user,
                LocalDateTime.now()
        );

        TransferRequest saved = transferRequestRepository.save(request);
        return mapToTransferResponseDto(saved);
    }

    @Transactional
    public OperationsDto.TransferResponseDto handleTransferStatus(
            Long id, String action, String username) {
        requireAdmin(username);

        TransferRequest transfer = transferRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Transfer request not found with id: " + id));
        if (action == null) {
            throw new RuntimeException("Action is required");
        }

        String normalizedAction = action.toUpperCase();

        if ("APPROVE".equals(normalizedAction)) {
            if (transfer.getStatus() != TransferRequest.Status.PENDING) {
                throw new RuntimeException(
                        "Only pending transfer requests can be approved");
            }

            Stock sourceStock = stockRepository.findByBranchIdAndProductId(
                            transfer.getSourceBranch().getId(),
                            transfer.getProduct().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "Source branch has no stock record for product: "
                                    + transfer.getProduct().getName()));

            if (sourceStock.getQuantity() < transfer.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock in source branch. Available: "
                                + sourceStock.getQuantity()
                                + ", Requested: " + transfer.getQuantity());
            }

            Integer previousSourceQuantity = sourceStock.getQuantity();
            sourceStock.setQuantity(previousSourceQuantity - transfer.getQuantity());

            Stock savedSource = stockRepository.save(sourceStock);
            stockMovementService.recordMovement(
                    savedSource.getBranch(),
                    savedSource.getProduct(),
                    "TRANSFER_OUT",
                    -transfer.getQuantity(),
                    previousSourceQuantity,
                    savedSource.getQuantity(),
                    transfer.getId(),
                    username
            );
            createAutoPurchaseRequest(savedSource, previousSourceQuantity);

            transfer.setStatus(TransferRequest.Status.APPROVED);

        } else if ("COMPLETE".equals(normalizedAction)) {
            if (transfer.getStatus() != TransferRequest.Status.APPROVED) {
                throw new RuntimeException(
                        "Only approved transfer requests can be marked as completed");
            }

            Stock destStock = stockRepository.findByBranchIdAndProductId(
                            transfer.getDestinationBranch().getId(),
                            transfer.getProduct().getId())
                    .orElseGet(() -> new Stock(
                            null,
                            transfer.getDestinationBranch(),
                            transfer.getProduct(),
                            0));

            boolean newDestinationStock = destStock.getId() == null;
            Integer previousDestinationQuantity = destStock.getQuantity();
            destStock.setQuantity(previousDestinationQuantity + transfer.getQuantity());

            Stock savedDestination = stockRepository.save(destStock);
            stockMovementService.recordMovement(
                    savedDestination.getBranch(),
                    savedDestination.getProduct(),
                    "TRANSFER_IN",
                    transfer.getQuantity(),
                    previousDestinationQuantity,
                    savedDestination.getQuantity(),
                    transfer.getId(),
                    username
            );
            createAutoPurchaseRequest(savedDestination,
                    newDestinationStock ? null : previousDestinationQuantity);

            transfer.setStatus(TransferRequest.Status.COMPLETED);

        } else if ("REJECT".equals(normalizedAction)) {
            if (transfer.getStatus() == TransferRequest.Status.APPROVED) {
                Stock sourceStock = stockRepository.findByBranchIdAndProductId(
                                transfer.getSourceBranch().getId(),
                                transfer.getProduct().getId())
                        .orElseGet(() -> new Stock(
                                null,
                                transfer.getSourceBranch(),
                                transfer.getProduct(),
                                0));

                int previousSourceQuantity = sourceStock.getQuantity();
                sourceStock.setQuantity(previousSourceQuantity + transfer.getQuantity());

                Stock savedSource = stockRepository.save(sourceStock);
                stockMovementService.recordMovement(
                        savedSource.getBranch(),
                        savedSource.getProduct(),
                        "TRANSFER_REVERSAL",
                        transfer.getQuantity(),
                        previousSourceQuantity,
                        savedSource.getQuantity(),
                        transfer.getId(),
                        username
                );
                transfer.setStatus(TransferRequest.Status.REJECTED);

            } else if (transfer.getStatus() == TransferRequest.Status.PENDING) {
                transfer.setStatus(TransferRequest.Status.REJECTED);

            } else {
                throw new RuntimeException(
                        "Cannot reject transfer request with status: "
                                + transfer.getStatus());
            }

        } else {
            throw new RuntimeException("Invalid action: " + action);
        }

        TransferRequest saved = transferRequestRepository.save(transfer);

        return mapToTransferResponseDto(saved);
    }

    public List<OperationsDto.PurchaseResponseDto> getPurchaseRequests(Long branchId, String username) {
        User user = getAuthenticatedUser(username);
        List<PurchaseRequest> list;

        if (user.getRole() == User.Role.ADMIN) {
            list = branchId != null
                    ? purchaseRequestRepository.findByBranchWithDetails(branchId)
                    : purchaseRequestRepository.findAllWithDetails();
        } else if (user.getAssignedBranch() == null) {
            list = List.of();
        } else {
            list = purchaseRequestRepository.findByBranchWithDetails(user.getAssignedBranch().getId());
        }

        return list.stream()
                .map(this::mapToPurchaseResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public OperationsDto.PurchaseResponseDto createPurchaseRequest(
            OperationsDto.PurchaseCreateDto dto, String username) {
        if (dto.getBranchId() == null || dto.getProductId() == null) {
            throw new RuntimeException("Branch and product are required");
        }

        requireBranchAccess(username, dto.getBranchId());

        if (dto.getRequestedQuantity() == null || dto.getRequestedQuantity() <= 0) {
            throw new RuntimeException("Purchase quantity must be greater than zero");
        }

        Branch branch = branchRepository.findById(dto.getBranchId())
                .orElseThrow(() -> new RuntimeException("Branch not found: " + dto.getBranchId()));

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found: " + dto.getProductId()));

        PurchaseRequest request = new PurchaseRequest(
                null,
                branch,
                product,
                dto.getRequestedQuantity(),
                PurchaseRequest.Status.PENDING,
                LocalDateTime.now()
        );

        PurchaseRequest saved = purchaseRequestRepository.save(request);
        return mapToPurchaseResponseDto(saved);
    }

    @Transactional
    public OperationsDto.PurchaseResponseDto handlePurchaseStatus(
            Long id, String action, String username) {
        requireAdmin(username);

        PurchaseRequest purchase = purchaseRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(
                        "Purchase request not found with id: " + id));

        if (purchase.getStatus() != PurchaseRequest.Status.PENDING) {
            throw new RuntimeException("Only pending purchase requests can be updated");
        }

        if (action == null) {
            throw new RuntimeException("Action is required");
        }

        String normalizedAction = action.toUpperCase();

        if ("APPROVE".equals(normalizedAction)) {
            purchase.setStatus(PurchaseRequest.Status.APPROVED);
        } else if ("REJECT".equals(normalizedAction)) {
            purchase.setStatus(PurchaseRequest.Status.REJECTED);
        } else {
            throw new RuntimeException("Invalid action: " + action);
        }

        PurchaseRequest saved = purchaseRequestRepository.save(purchase);
        return mapToPurchaseResponseDto(saved);
    }

    private User getAuthenticatedUser(String username) {
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Authentication is required");
        }
        return userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    private void requireAdmin(String username) {
        User user = getAuthenticatedUser(username);
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Only Admin can approve, reject or complete requests");
        }
    }

    private void requireBranchAccess(String username, Long branchId) {
        if (branchId == null) {
            throw new RuntimeException("Branch is required");
        }

        User user = getAuthenticatedUser(username);
        if (user.getRole() == User.Role.ADMIN) {
            return;
        }

        if (user.getAssignedBranch() == null
                || !user.getAssignedBranch().getId().equals(branchId)) {
            throw new RuntimeException("You can perform this action only for your assigned branch");
        }
    }

    public List<OperationsDto.StockMovementDto> getStockMovements(Long branchId, String username) {
        return stockMovementService.getMovements(branchId, username);
    }

    public List<OperationsDto.StockDto> getStocksForExport(Long branchId, String username) {
        User user = getAuthenticatedUser(username);
        if (user.getRole() == User.Role.ADMIN) {
            return getStocks(branchId);
        }

        if (user.getAssignedBranch() == null) {
            return List.of();
        }

        Long assignedBranchId = user.getAssignedBranch().getId();
        if (branchId != null && !branchId.equals(assignedBranchId)) {
            throw new RuntimeException("You can export stock only for your assigned branch");
        }

        return getStocks(assignedBranchId);
    }

    public OperationsDto.StockDto mapToStockDto(Stock stock) {
        Integer threshold = stock.getProduct().getMinimumStockThreshold();
        boolean isLow = threshold != null && threshold > 0
                && stock.getQuantity() <= threshold;

        return new OperationsDto.StockDto(
                stock.getId(),
                stock.getBranch().getId(),
                stock.getBranch().getName(),
                stock.getProduct().getId(),
                stock.getProduct().getName(),
                stock.getProduct().getSku(),
                stock.getQuantity(),
                stock.getProduct().getMinimumStockThreshold(),
                isLow
        );
    }

    private OperationsDto.TransferResponseDto mapToTransferResponseDto(
            TransferRequest tr) {
        Long reqById = tr.getRequestedBy() != null
                ? tr.getRequestedBy().getId() : null;

        String reqByName = tr.getRequestedBy() != null
                ? tr.getRequestedBy().getName() : null;

        return new OperationsDto.TransferResponseDto(
                tr.getId(),
                tr.getSourceBranch().getId(),
                tr.getSourceBranch().getName(),
                tr.getDestinationBranch().getId(),
                tr.getDestinationBranch().getName(),
                tr.getProduct().getId(),
                tr.getProduct().getName(),
                tr.getProduct().getSku(),
                tr.getQuantity(),
                tr.getStatus().name(),
                reqById,
                reqByName,
                tr.getCreatedAt()
        );
    }

    private OperationsDto.PurchaseResponseDto mapToPurchaseResponseDto(
            PurchaseRequest pr) {
        return new OperationsDto.PurchaseResponseDto(
                pr.getId(),
                pr.getBranch().getId(),
                pr.getBranch().getName(),
                pr.getProduct().getId(),
                pr.getProduct().getName(),
                pr.getProduct().getSku(),
                pr.getRequestedQuantity(),
                pr.getStatus().name(),
                pr.getCreatedAt()
        );
    }
}
