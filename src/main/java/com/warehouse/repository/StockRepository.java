package com.warehouse.repository;

import com.warehouse.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByBranchIdAndProductId(Long branchId, Long productId);

    List<Stock> findByBranchId(Long branchId);

    List<Stock> findByProductId(Long productId);

    @Query("SELECT s FROM Stock s JOIN FETCH s.branch JOIN FETCH s.product WHERE s.quantity <= s.product.minimumStockThreshold")
    List<Stock> findLowStockItems();

    @Query("SELECT COUNT(s) FROM Stock s WHERE s.quantity <= s.product.minimumStockThreshold")
    long countLowStockItems();
}
