package com.warehouse.repository;

import com.warehouse.entity.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {

    List<PurchaseRequest> findByStatus(PurchaseRequest.Status status);

    long countByStatus(PurchaseRequest.Status status);

    @Query("SELECT p FROM PurchaseRequest p JOIN FETCH p.branch JOIN FETCH p.product ORDER BY p.createdAt DESC")
    List<PurchaseRequest> findAllWithDetails();

    @Query("SELECT p FROM PurchaseRequest p JOIN FETCH p.branch JOIN FETCH p.product WHERE p.branch.id = :branchId ORDER BY p.createdAt DESC")
    List<PurchaseRequest> findByBranchWithDetails(@Param("branchId") Long branchId);

    boolean existsByBranch_IdAndProduct_IdAndStatusIn(
        Long branchId,
        Long productId,
        List<PurchaseRequest.Status> statuses
);
}
