package com.lab.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lab.marketplace.dto.UserResponse;
import com.lab.marketplace.entity.Order;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.exception.ResourceNotFoundException;
import com.lab.marketplace.repository.OrderRepository;
import com.lab.marketplace.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void toggleUserStatusFlipsEnabledFlagAndSaves() {
        User user = User.builder().id(1L).username("test").enabled(true).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        adminService.toggleUserStatus(1L);

        assertThat(user.getEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void toggleUserStatusThrowsWhenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.toggleUserStatus(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUsersByRoleReturnsMappedResponseList() {
        User seller = User.builder().id(7L).username("seller").email("s@test.com").enabled(true).build();
        when(userRepository.findByRoleName("SELLER")).thenReturn(List.of(seller));

        List<UserResponse> responses = adminService.getUsersByRole("SELLER");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getUsername()).isEqualTo("seller");
    }

    @Test
    void getAllOrdersReturnsRepositoryData() {
        Order order = Order.builder().id(33L).build();
        when(orderRepository.findAll()).thenReturn(List.of(order));

        List<Order> orders = adminService.getAllOrders();

        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).getId()).isEqualTo(33L);
    }

    @Test
    void totalCountersDelegateToRepositories() {
        when(userRepository.count()).thenReturn(10L);
        when(orderRepository.count()).thenReturn(14L);

        assertThat(adminService.getTotalUsers()).isEqualTo(10L);
        assertThat(adminService.getTotalOrders()).isEqualTo(14L);
    }
}

