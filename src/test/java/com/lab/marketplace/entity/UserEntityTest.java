package com.lab.marketplace.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserEntityTest {

    @Test
    void addRoleAddsRoleToBothSides() {
        User user = User.builder().username("alice").build();
        Role role = Role.builder().name(Role.SELLER).build();

        user.addRole(role);

        assertThat(user.getRoles()).contains(role);
        assertThat(role.getUsers()).contains(user);
    }

    @Test
    void removeRoleRemovesRoleFromBothSides() {
        User user = User.builder().username("bob").build();
        Role role = Role.builder().name(Role.BUYER).build();
        user.addRole(role);

        user.removeRole(role);

        assertThat(user.getRoles()).doesNotContain(role);
        assertThat(role.getUsers()).doesNotContain(user);
    }

    @Test
    void hasRoleReturnsTrueOnlyForExistingRole() {
        User user = User.builder().username("charlie").build();
        user.addRole(Role.builder().name(Role.ADMIN).build());

        assertThat(user.hasRole(Role.ADMIN)).isTrue();
        assertThat(user.hasRole(Role.SELLER)).isFalse();
    }
}

