package com.lab.marketplace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;

class ProductResponseTest {

    @Test
    void fromEntityMapsAllExpectedFields() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(2);
        LocalDateTime updatedAt = LocalDateTime.now();

        User seller = User.builder().id(88L).username("seller88").build();
        Product product = Product.builder()
                .id(9L)
                .name("Camera")
                .description("DSLR camera")
                .price(new BigDecimal("899.99"))
                .stock(4)
                .category("Electronics")
                .imageUrl("camera.png")
                .seller(seller)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();

        ProductResponse response = ProductResponse.fromEntity(product);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getName()).isEqualTo("Camera");
        assertThat(response.getSellerId()).isEqualTo(88L);
        assertThat(response.getSellerUsername()).isEqualTo("seller88");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }
}

