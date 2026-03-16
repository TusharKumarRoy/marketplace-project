package com.lab.marketplace.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ProductEntityTest {

    @Test
    void isInStockReturnsTrueWhenStockPositive() {
        Product product = Product.builder().name("Laptop").price(new BigDecimal("10.00")).stock(3).build();

        assertThat(product.isInStock()).isTrue();
    }

    @Test
    void isInStockReturnsFalseWhenStockZero() {
        Product product = Product.builder().name("Phone").price(new BigDecimal("10.00")).stock(0).build();

        assertThat(product.isInStock()).isFalse();
    }

    @Test
    void decreaseStockReducesStockWhenEnoughQuantity() {
        Product product = Product.builder().stock(10).build();

        product.decreaseStock(4);

        assertThat(product.getStock()).isEqualTo(6);
    }

    @Test
    void decreaseStockThrowsWhenQuantityExceedsStock() {
        Product product = Product.builder().stock(2).build();

        assertThatThrownBy(() -> product.decreaseStock(3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Insufficient stock");
    }

    @Test
    void increaseStockAddsQuantity() {
        Product product = Product.builder().stock(5).build();

        product.increaseStock(7);

        assertThat(product.getStock()).isEqualTo(12);
    }
}

