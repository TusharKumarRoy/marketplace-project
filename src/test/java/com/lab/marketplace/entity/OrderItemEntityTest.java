package com.lab.marketplace.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderItemEntityTest {

    @Test
    void calculateSubtotalMultipliesUnitPriceByQuantity() {
        OrderItem item = OrderItem.builder()
                .unitPrice(new BigDecimal("7.25"))
                .quantity(4)
                .build();

        item.calculateSubtotal();

        assertThat(item.getSubtotal()).isEqualByComparingTo("29.00");
    }

    @Test
    void getSubtotalCalculatesWhenSubtotalIsNull() {
        OrderItem item = OrderItem.builder()
                .unitPrice(new BigDecimal("3.50"))
                .quantity(3)
                .build();

        BigDecimal subtotal = item.getSubtotal();

        assertThat(subtotal).isEqualByComparingTo("10.50");
    }
}

