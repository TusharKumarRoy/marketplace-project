package com.lab.marketplace.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadUserByUsernameBuildsSpringUserWithAuthorities() {
        User user = User.builder()
                .username("alice")
                .password("encoded")
                .enabled(true)
                .build();
        user.addRole(Role.builder().name(Role.SELLER).build());

        when(userRepository.findByUsernameOrEmail("alice")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("alice");

        assertThat(details.getUsername()).isEqualTo("alice");
        assertThat(details.getPassword()).isEqualTo("encoded");
        assertThat(details.getAuthorities())
                .extracting("authority")
                .contains("ROLE_SELLER");
    }

    @Test
    void loadUserByUsernameThrowsWhenUserMissing() {
        when(userRepository.findByUsernameOrEmail("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found: missing");
    }
}

