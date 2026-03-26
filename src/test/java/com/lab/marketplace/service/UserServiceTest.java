package com.lab.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.lab.marketplace.dto.LoginRequest;
import com.lab.marketplace.dto.RegisterRequest;
import com.lab.marketplace.dto.UserResponse;
import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.exception.BadRequestException;
import com.lab.marketplace.repository.RoleRepository;
import com.lab.marketplace.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerThrowsWhenUsernameAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .username("taken")
                .email("new@test.com")
                .password("password")
                .fullName("Taken User")
                .role(Role.BUYER)
                .build();

        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Username already exists");
    }

    @Test
    void registerSavesEncodedPasswordAndReturnsResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .username("newuser")
                .email("new@test.com")
                .password("secret")
                .fullName("New User")
                .phoneNumber("0123456789")
                .role(Role.SELLER)
                .build();

        Role sellerRole = Role.builder().name(Role.SELLER).build();
        User saved = User.builder()
                .id(5L)
                .username("newuser")
                .email("new@test.com")
                .password("ENC(secret)")
                .fullName("New User")
                .enabled(true)
                .build();
        saved.addRole(sellerRole);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(roleRepository.findByName(Role.SELLER)).thenReturn(Optional.of(sellerRole));
        when(passwordEncoder.encode("secret")).thenReturn("ENC(secret)");
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponse response = userService.register(request);

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getUsername()).isEqualTo("newuser");
        assertThat(response.getRoles()).contains(Role.SELLER);
    }

    @Test
    void loginThrowsWhenPasswordDoesNotMatch() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("user1")
                .password("wrong")
                .build();

        User existing = User.builder()
                .username("user1")
                .email("u1@test.com")
                .password("hash")
                .enabled(true)
                .build();

        when(userRepository.findByUsernameOrEmail("user1")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Invalid username/email or password");
    }

    @Test
    void loginReturnsUserResponseWhenCredentialsAreValid() {
        LoginRequest request = LoginRequest.builder()
                .usernameOrEmail("user2")
                .password("pw")
                .build();

        User existing = User.builder()
                .id(9L)
                .username("user2")
                .email("u2@test.com")
                .password("hash")
                .enabled(true)
                .build();
        existing.addRole(Role.builder().name(Role.BUYER).build());

        when(userRepository.findByUsernameOrEmail("user2")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);

        UserResponse response = userService.login(request);

        assertThat(response.getId()).isEqualTo(9L);
        assertThat(response.getUsername()).isEqualTo("user2");
    }
}

