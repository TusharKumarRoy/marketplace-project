package com.lab.marketplace.controller.web;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lab.marketplace.entity.Order;
import com.lab.marketplace.entity.OrderItem;
import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.OrderRepository;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerWebController {

    private record RecentOrderRow(Long id, String buyerUsername, int itemCount, BigDecimal totalAmount, Order.OrderStatus status) {}

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    
    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(Authentication authentication, Model model) {
    String username = authentication.getName();
    log.info("Seller dashboard accessed by: {}", username);

    User seller = userRepository.findByUsername(username)
        .orElseThrow(() -> new IllegalStateException("Authenticated seller not found"));

    List<Product> products = productRepository.findBySellerId(seller.getId());
    List<Order> sellerOrders = orderRepository.findOrdersForSeller(seller.getId(), Order.OrderStatus.CANCELLED);

    long totalProducts = products.size();
    long activeProducts = products.stream().filter(product -> Boolean.TRUE.equals(product.getAvailable())).count();
    long lowStockProducts = products.stream()
        .filter(product -> product.getStock() != null && product.getStock() > 0 && product.getStock() <= 5)
        .count();

    long totalOrders = sellerOrders.size();
    long pendingOrders = sellerOrders.stream()
        .filter(order -> order.getStatus() == Order.OrderStatus.PENDING
            || order.getStatus() == Order.OrderStatus.CONFIRMED
            || order.getStatus() == Order.OrderStatus.PROCESSING)
        .count();

    BigDecimal totalRevenue = sellerOrders.stream()
        .filter(order -> order.getStatus() == Order.OrderStatus.DELIVERED)
        .flatMap(order -> order.getOrderItems().stream())
        .filter(item -> item.getProduct() != null && item.getProduct().getSeller() != null
            && seller.getId().equals(item.getProduct().getSeller().getId()))
        .map(OrderItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<Product> recentProducts = products.stream()
        .sorted(Comparator.comparing(Product::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .limit(5)
        .toList();

    List<RecentOrderRow> recentOrders = sellerOrders.stream()
        .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .limit(5)
        .map(order -> new RecentOrderRow(
            order.getId(),
            order.getBuyer() != null && order.getBuyer().getUsername() != null ? order.getBuyer().getUsername() : "Unknown buyer",
            order.getOrderItems() != null ? order.getOrderItems().size() : 0,
            order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO,
            order.getStatus() != null ? order.getStatus() : Order.OrderStatus.PENDING
        ))
        .toList();

    model.addAttribute("username", username);
    model.addAttribute("totalProducts", totalProducts);
    model.addAttribute("activeProducts", activeProducts);
    model.addAttribute("lowStockProducts", lowStockProducts);
    model.addAttribute("totalOrders", totalOrders);
    model.addAttribute("pendingOrders", pendingOrders);
    model.addAttribute("totalRevenue", totalRevenue);
    model.addAttribute("recentProducts", recentProducts);
    model.addAttribute("recentOrders", recentOrders);
    model.addAttribute("statusOptions", List.of(
        Order.OrderStatus.CONFIRMED,
        Order.OrderStatus.PROCESSING,
        Order.OrderStatus.SHIPPED,
        Order.OrderStatus.DELIVERED
    ));
        return "seller/dashboard";
    }
}
