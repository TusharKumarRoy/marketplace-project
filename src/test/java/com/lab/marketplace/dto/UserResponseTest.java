package com.lab.marketplace.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;

class UserResponseTest {

    @Test
    void fromEntityMapsUserFieldsAndRoleNames() {
        Role sellerRole = Role.builder().name(Role.SELLER).build();
        Role buyerRole = Role.builder().name(Role.BUYER).build();

        User user = User.builder()
                .id(11L)
                .username("masum")
                .email("masum@test.com")
                .fullName("Masum Tester")
                .phoneNumber("12345")
                .enabled(true)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
        user.addRole(sellerRole);
        user.addRole(buyerRole);

        UserResponse response = UserResponse.fromEntity(user);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getUsername()).isEqualTo("masum");
        assertThat(response.getEmail()).isEqualTo("masum@test.com");
        assertThat(response.getRoles()).containsExactlyInAnyOrder(Role.SELLER, Role.BUYER);
        assertThat(response.getEnabled()).isTrue();
    }
}

