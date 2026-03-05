package com.lab.marketplace.repository;

import com.lab.marketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Find all products by seller
     */
    List<Product> findBySellerId(Long sellerId);

    /**
     * Find all available products
     */
    List<Product> findByAvailableTrue();

    /**
     * Find products by category
     */
    List<Product> findByCategory(String category);

    /**
     * Find products by name containing (search)
     */
    List<Product> findByNameContainingIgnoreCase(String name);

    /**
     * Find products in stock
     */
    @Query("SELECT p FROM Product p WHERE p.stock > 0 AND p.available = true")
    List<Product> findInStockProducts();

    /**
     * Find products by seller and availability
     */
    List<Product> findBySellerIdAndAvailable(Long sellerId, Boolean available);
}
