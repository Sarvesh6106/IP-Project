package com.warehouse.repository;

import com.warehouse.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.branch JOIN FETCH m.product ORDER BY m.createdAt DESC")
    List<StockMovement> findAllWithDetails();

    @Query("SELECT m FROM StockMovement m JOIN FETCH m.branch JOIN FETCH m.product WHERE m.branch.id = :branchId ORDER BY m.createdAt DESC")
    List<StockMovement> findByBranchIdWithDetails(@Param("branchId") Long branchId);
}
