package com.lab.marketplace.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class OrderEntityTest {

    @Test
    void addOrderItemSetsBidirectionalReference() {
        Order order = Order.builder().build();
        OrderItem item = OrderItem.builder().quantity(1).unitPrice(new BigDecimal("12.00")).build();

        order.addOrderItem(item);

        assertThat(order.getOrderItems()).contains(item);
        assertThat(item.getOrder()).isEqualTo(order);
    }

    @Test
    void removeOrderItemClearsBidirectionalReference() {
        Order order = Order.builder().build();
        OrderItem item = OrderItem.builder().quantity(1).unitPrice(new BigDecimal("12.00")).build();
        order.addOrderItem(item);

        order.removeOrderItem(item);

        assertThat(order.getOrderItems()).doesNotContain(item);
        assertThat(item.getOrder()).isNull();
    }

    @Test
    void calculateTotalAmountSumsAllItemSubtotals() {
        OrderItem first = OrderItem.builder().subtotal(new BigDecimal("10.50")).build();
        OrderItem second = OrderItem.builder().subtotal(new BigDecimal("20.25")).build();
        Order order = Order.builder().build();
        order.addOrderItem(first);
        order.addOrderItem(second);

        order.calculateTotalAmount();

        assertThat(order.getTotalAmount()).isEqualByComparingTo("30.75");
    }
}

