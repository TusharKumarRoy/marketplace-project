package com.lab.marketplace.controller.web;

import com.lab.marketplace.dto.UserResponse;
import com.lab.marketplace.entity.Order;
import com.lab.marketplace.service.AdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminWebController {
    
    private final AdminService adminService;
    
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        log.info("Loading admin dashboard");
        
        long totalUsers = adminService.getTotalUsers();
        long totalOrders = adminService.getTotalOrders();
        
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalOrders", totalOrders);
        
        return "admin/dashboard";
    }
    
    @GetMapping("/users")
    public String usersPage(@RequestParam(required = false) String role, Model model) {
        log.info("Loading users page with role filter: {}", role);
        
        List<UserResponse> users;
        if (role != null && !role.isEmpty()) {
            users = adminService.getUsersByRole(role);
        } else {
            users = adminService.getAllUsers();
        }
        
        model.addAttribute("users", users);
        model.addAttribute("selectedRole", role);
        
        return "admin/users";
    }
    
    @PostMapping("/users/{userId}/toggle-status")
    public String toggleUserStatus(@PathVariable Long userId) {
        log.info("Toggling status for user ID: {}", userId);
        adminService.toggleUserStatus(userId);
        return "redirect:/admin/users";
    }
    
    @GetMapping("/orders")
    public String ordersPage(Model model) {
        log.info("Loading admin orders page");
        
        List<Order> orders = adminService.getAllOrders();
        model.addAttribute("orders", orders);
        
        return "admin/orders";
    }
}
