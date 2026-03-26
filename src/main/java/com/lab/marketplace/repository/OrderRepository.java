package com.lab.marketplace.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.lab.marketplace.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find all orders by buyer
     */
    List<Order> findByBuyerId(Long buyerId);

    /**
     * Find all orders by status
     */
    List<Order> findByStatus(Order.OrderStatus status);

    /**
     * Find orders by buyer and status
     */
    List<Order> findByBuyerIdAndStatus(Long buyerId, Order.OrderStatus status);

    /**
     * Find all orders for products sold by a specific seller
     */
    @Query("SELECT DISTINCT o FROM Order o JOIN o.orderItems oi JOIN oi.product p WHERE p.seller.id = :sellerId AND o.status <> :excludedStatus")
    List<Order> findOrdersForSeller(@Param("sellerId") Long sellerId,
                                    @Param("excludedStatus") Order.OrderStatus excludedStatus);

    /**
     * Find orders by buyer ordered by created date descending
     */
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
