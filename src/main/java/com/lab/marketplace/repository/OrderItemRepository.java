package com.lab.marketplace.repository;

import com.lab.marketplace.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Find all order items by order ID
     */
    List<OrderItem> findByOrderId(Long orderId);

    /**
     * Find all order items by product ID
     */
    List<OrderItem> findByProductId(Long productId);

    /**
     * Get total quantity sold for a product
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.product.id = :productId")
    Long getTotalQuantitySoldForProduct(@Param("productId") Long productId);
}
