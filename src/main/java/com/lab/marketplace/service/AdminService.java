package com.lab.marketplace.service;

import com.lab.marketplace.dto.UserResponse;
import com.lab.marketplace.entity.Order;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.exception.ResourceNotFoundException;
import com.lab.marketplace.repository.OrderRepository;
import com.lab.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {
    
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users");
        return userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String roleName) {
        log.info("Fetching users with role: {}", roleName);
        return userRepository.findByRoleName(roleName).stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public void toggleUserStatus(Long userId) {
        log.info("Toggling user status for user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
        
        log.info("User {} status changed to: {}", user.getUsername(), user.getEnabled());
    }
    
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.info("Fetching all orders with buyer information");
        return orderRepository.findAllWithBuyer();
    }
    
    @Transactional(readOnly = true)
    public long getTotalUsers() {
        return userRepository.count();
    }
    
    @Transactional(readOnly = true)
    public long getTotalOrders() {
        return orderRepository.count();
    }
}
