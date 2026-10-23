package com.warehouse.repository;

import com.warehouse.entity.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    List<TransferRequest> findByStatus(TransferRequest.Status status);

    long countByStatus(TransferRequest.Status status);

    @Query("SELECT t FROM TransferRequest t JOIN FETCH t.sourceBranch JOIN FETCH t.destinationBranch JOIN FETCH t.product LEFT JOIN FETCH t.requestedBy ORDER BY t.createdAt DESC")
    List<TransferRequest> findAllWithDetails();

    @Query("SELECT t FROM TransferRequest t JOIN FETCH t.sourceBranch JOIN FETCH t.destinationBranch JOIN FETCH t.product LEFT JOIN FETCH t.requestedBy WHERE t.sourceBranch.id = :branchId OR t.destinationBranch.id = :branchId ORDER BY t.createdAt DESC")
    List<TransferRequest> findByBranchWithDetails(@Param("branchId") Long branchId);
}
