package com.warehouse.service;

import com.warehouse.dto.OperationsDto;
import com.warehouse.entity.Branch;
import com.warehouse.entity.Product;
import com.warehouse.entity.StockMovement;
import com.warehouse.entity.User;
import com.warehouse.repository.StockMovementRepository;
import com.warehouse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    @Transactional
    public void recordMovement(Branch branch, Product product, String movementType,
                               int quantityChange, int previousQuantity, int newQuantity,
                               Long referenceId, String username) {
        if (quantityChange == 0) {
            return;
        }

        String performedBy = "System";
        if (username != null && !username.isBlank()) {
            performedBy = userRepository.findByEmail(username)
                    .map(User::getName)
                    .orElse(username);
        }

        StockMovement movement = new StockMovement(
                null,
                branch,
                product,
                movementType,
                quantityChange,
                previousQuantity,
                newQuantity,
                referenceId,
                performedBy,
                LocalDateTime.now()
        );

        stockMovementRepository.save(movement);
    }

    @Transactional(readOnly = true)
    public List<OperationsDto.StockMovementDto> getMovements(Long branchId, String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        List<StockMovement> movements;
        if (user.getRole() == User.Role.ADMIN) {
            movements = branchId == null
                    ? stockMovementRepository.findAllWithDetails()
                    : stockMovementRepository.findByBranchIdWithDetails(branchId);
        } else {
            if (user.getAssignedBranch() == null) {
                return List.of();
            }

            Long assignedBranchId = user.getAssignedBranch().getId();
            if (branchId != null && !branchId.equals(assignedBranchId)) {
                throw new RuntimeException("You can view stock history only for your assigned branch");
            }

            movements = stockMovementRepository.findByBranchIdWithDetails(assignedBranchId);
        }

        return movements.stream().map(movement -> new OperationsDto.StockMovementDto(
                movement.getId(),
                movement.getBranch().getId(),
                movement.getBranch().getName(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getMovementType(),
                movement.getQuantityChange(),
                movement.getPreviousQuantity(),
                movement.getNewQuantity(),
                movement.getReferenceId(),
                movement.getPerformedBy(),
                movement.getCreatedAt()
        )).collect(Collectors.toList());
    }
}
