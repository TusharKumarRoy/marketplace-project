package com.lab.marketplace.controller.web;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lab.marketplace.entity.Order;
import com.lab.marketplace.entity.OrderItem;
import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.OrderRepository;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class OrderWebController {

        private static final String CART_SESSION_KEY = "MARKETPLACE_CART";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping("/buyer/orders")
    @PreAuthorize("hasRole('BUYER')")
        @Transactional(readOnly = true)
    public String buyerOrders(Principal principal, Model model) {
        User buyer = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated buyer not found"));

        List<BuyerOrderView> orders = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId())
                .stream()
                .map(order -> new BuyerOrderView(
                        order.getId(),
                        order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                        order.getTotalAmount(),
                        order.getStatus(),
                        canBuyerCancel(order.getStatus())))
                .toList();

        model.addAttribute("orders", orders);
        return "buyer/orders";
    }

    @GetMapping("/seller/orders")
    @PreAuthorize("hasRole('SELLER')")
        @Transactional(readOnly = true)
    public String sellerOrders(Principal principal, Model model) {
        User seller = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated seller not found"));

        List<SellerOrderView> orders = orderRepository.findOrdersForSeller(seller.getId(), Order.OrderStatus.CANCELLED)
                .stream()
                .map(order -> new SellerOrderView(
                        order.getId(),
                        order.getBuyer().getUsername(),
                        order.getOrderItems().stream().mapToInt(OrderItem::getQuantity).sum(),
                        order.getTotalAmount(),
                        order.getStatus()))
                .toList();

                model.addAttribute("statusOptions", getSellerUpdatableStatuses());
        model.addAttribute("orders", orders);
        return "seller/orders";
    }

        @PostMapping("/seller/orders/{orderId}/status")
        @PreAuthorize("hasRole('SELLER')")
        @Transactional
        public String updateSellerOrderStatus(@PathVariable Long orderId,
                                                                                  @RequestParam Order.OrderStatus status,
                                                                                  @RequestParam(defaultValue = "/seller/orders") String redirectPath,
                                                                                  Principal principal,
                                                                                  RedirectAttributes redirectAttributes) {
                User seller = userRepository.findByUsername(principal.getName())
                                .orElseThrow(() -> new IllegalStateException("Authenticated seller not found"));

                if (!getSellerUpdatableStatuses().contains(status)) {
                        redirectAttributes.addFlashAttribute("error", "Invalid status selected.");
                        return "redirect:" + normalizeSellerRedirect(redirectPath);
                }

                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                boolean sellerOwnsAnyOrderItem = order.getOrderItems().stream()
                                .anyMatch(item -> item.getProduct() != null
                                                && item.getProduct().getSeller() != null
                                                && seller.getId().equals(item.getProduct().getSeller().getId()));

                if (!sellerOwnsAnyOrderItem) {
                        redirectAttributes.addFlashAttribute("error", "You are not allowed to update this order.");
                        return "redirect:" + normalizeSellerRedirect(redirectPath);
                }

                if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.DELIVERED) {
                        redirectAttributes.addFlashAttribute("error", "This order status can no longer be changed.");
                        return "redirect:" + normalizeSellerRedirect(redirectPath);
                }

                order.setStatus(status);
                orderRepository.save(order);

                redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " status updated to " + status + ".");
                return "redirect:" + normalizeSellerRedirect(redirectPath);
        }

        @PostMapping("/buyer/orders/{orderId}/cancel")
        @PreAuthorize("hasRole('BUYER')")
        @Transactional
        public String cancelBuyerOrder(@PathVariable Long orderId,
                                                                   Principal principal,
                                                                   RedirectAttributes redirectAttributes) {
                User buyer = userRepository.findByUsername(principal.getName())
                                .orElseThrow(() -> new IllegalStateException("Authenticated buyer not found"));

                Order order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

                if (!order.getBuyer().getId().equals(buyer.getId())) {
                        redirectAttributes.addFlashAttribute("error", "You can only cancel your own orders.");
                        return "redirect:/buyer/orders";
                }

                if (!canBuyerCancel(order.getStatus())) {
                        redirectAttributes.addFlashAttribute("error", "This order can no longer be cancelled.");
                        return "redirect:/buyer/orders";
                }

                for (OrderItem item : order.getOrderItems()) {
                        Product product = item.getProduct();
                        if (product == null || item.getQuantity() == null) {
                                continue;
                        }

                        product.increaseStock(item.getQuantity());
                        product.setAvailable(product.getStock() != null && product.getStock() > 0);
                        productRepository.save(product);
                }

                order.setStatus(Order.OrderStatus.CANCELLED);
                orderRepository.save(order);

                redirectAttributes.addFlashAttribute("success", "Order #" + orderId + " cancelled successfully.");
                return "redirect:/buyer/orders";
        }

        @GetMapping("/cart")
        @PreAuthorize("hasRole('BUYER')")
        public String viewCart(HttpSession session, Model model) {
                Map<Long, Integer> cart = getOrCreateCart(session);
                List<CheckoutItemView> checkoutItems = buildCheckoutItemsFromCart(cart);

                BigDecimal subtotal = calculateSubtotal(checkoutItems);
                BigDecimal shipping = calculateShipping(subtotal);

                model.addAttribute("checkoutItems", checkoutItems);
                model.addAttribute("subtotal", subtotal);
                model.addAttribute("shipping", shipping);
                model.addAttribute("total", subtotal.add(shipping));
                return "orders/cart";
        }

        @PostMapping("/cart/add")
        @PreAuthorize("hasRole('BUYER')")
        public String addToCart(@RequestParam Long productId,
                                                        @RequestParam(defaultValue = "1") Integer qty,
                                                        @RequestParam(defaultValue = "/cart") String redirect,
                                                        HttpSession session,
                                                        RedirectAttributes redirectAttributes) {
                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

                if (product.getStock() == null || product.getStock() <= 0 || Boolean.FALSE.equals(product.getAvailable())) {
                        redirectAttributes.addFlashAttribute("error", "This product is currently out of stock.");
                        return "redirect:" + redirect;
                }

                int safeQty = Math.max(1, qty == null ? 1 : qty);
                Map<Long, Integer> cart = getOrCreateCart(session);
                int existingQty = cart.getOrDefault(productId, 0);
                int updatedQty = Math.min(existingQty + safeQty, product.getStock());
                cart.put(productId, updatedQty);

                redirectAttributes.addFlashAttribute("success", "Added to cart: " + product.getName());
                return "redirect:" + redirect;
        }

        @PostMapping("/cart/update")
        @PreAuthorize("hasRole('BUYER')")
        public String updateCartItem(@RequestParam Long productId,
                                                                 @RequestParam Integer qty,
                                                                 HttpSession session) {
                Map<Long, Integer> cart = getOrCreateCart(session);
                if (!cart.containsKey(productId)) {
                        return "redirect:/cart";
                }

                if (qty == null || qty <= 0) {
                        cart.remove(productId);
                        return "redirect:/cart";
                }

                Product product = productRepository.findById(productId)
                                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

                int finalQty = Math.min(qty, Math.max(1, product.getStock()));
                cart.put(productId, finalQty);
                return "redirect:/cart";
        }

        @PostMapping("/cart/remove")
        @PreAuthorize("hasRole('BUYER')")
        public String removeCartItem(@RequestParam Long productId, HttpSession session) {
                Map<Long, Integer> cart = getOrCreateCart(session);
                cart.remove(productId);
                return "redirect:/cart";
        }

    @GetMapping("/orders/checkout")
    @PreAuthorize("hasRole('BUYER')")
    public String checkout(@RequestParam(required = false) Long productId,
                           @RequestParam(defaultValue = "1") Integer qty,
                                                   HttpSession session,
                           Model model) {
                List<CheckoutItemView> checkoutItems;
                Long selectedProductId = null;
                Integer selectedQty = null;

                if (productId != null) {
                        int safeQty = Math.max(1, qty == null ? 1 : qty);
                        Product product = productRepository.findById(productId)
                                        .orElseThrow(() -> new IllegalArgumentException("Product not found"));

                        int maxAllowedQty = product.getStock() == null ? safeQty : Math.max(1, product.getStock());
                        int finalQty = Math.min(safeQty, maxAllowedQty);
                        checkoutItems = List.of(createCheckoutItem(product, finalQty));

                        selectedProductId = productId;
                        selectedQty = finalQty;
                } else {
                        checkoutItems = buildCheckoutItemsFromCart(getOrCreateCart(session));
                }

                BigDecimal subtotal = calculateSubtotal(checkoutItems);
                BigDecimal shipping = calculateShipping(subtotal);

        model.addAttribute("checkoutItems", checkoutItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("shipping", shipping);
        model.addAttribute("total", subtotal.add(shipping));
                model.addAttribute("selectedProductId", selectedProductId);
                model.addAttribute("selectedQty", selectedQty);
        return "orders/checkout";
    }

    @PostMapping("/orders/place")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional
        public String placeOrder(@RequestParam(required = false) Long productId,
                                                         @RequestParam(defaultValue = "1") Integer qty,
                             @RequestParam String shippingAddress,
                             @RequestParam(required = false) String phone,
                             @RequestParam(required = false) String notes,
                             Principal principal,
                                                         HttpSession session,
                             RedirectAttributes redirectAttributes) {
        User buyer = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated buyer not found"));

                List<PurchaseItem> purchaseItems = new ArrayList<>();
                if (productId != null) {
                        Product product = productRepository.findById(productId)
                                        .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                        int requestedQty = Math.max(1, qty == null ? 1 : qty);
                        purchaseItems.add(new PurchaseItem(product, requestedQty));
                } else {
                        Map<Long, Integer> cart = getOrCreateCart(session);
                        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
                                Product product = productRepository.findById(entry.getKey())
                                                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
                                purchaseItems.add(new PurchaseItem(product, Math.max(1, entry.getValue())));
                        }
                }

                if (purchaseItems.isEmpty()) {
                        redirectAttributes.addFlashAttribute("error", "Your cart is empty.");
                        return "redirect:/cart";
                }

                for (PurchaseItem item : purchaseItems) {
                        if (item.product().getStock() == null || item.product().getStock() < item.qty()) {
                                redirectAttributes.addFlashAttribute("error",
                                                "Not enough stock for: " + item.product().getName());
                                if (productId != null) {
                                        return "redirect:/orders/checkout?productId=" + item.product().getId() + "&qty=" + item.qty();
                                }
                                return "redirect:/cart";
                        }
                }

        Order order = Order.builder()
                .buyer(buyer)
                .status(Order.OrderStatus.PENDING)
                .shippingAddress(shippingAddress)
                .notes(buildOrderNotes(phone, notes))
                .totalAmount(BigDecimal.ZERO)
                .build();

                for (PurchaseItem item : purchaseItems) {
                        Product product = item.product();
                        int requestedQty = item.qty();

                        OrderItem orderItem = OrderItem.builder()
                                        .product(product)
                                        .quantity(requestedQty)
                                        .unitPrice(product.getPrice())
                                        .build();
                        orderItem.calculateSubtotal();
                        order.addOrderItem(orderItem);

                        product.decreaseStock(requestedQty);
                        if (product.getStock() <= 0) {
                                product.setAvailable(false);
                        }
                        productRepository.save(product);
                }

        order.calculateTotalAmount();

        Order savedOrder = orderRepository.save(order);

                if (productId == null) {
                        getOrCreateCart(session).clear();
                }

                log.info("Order {} placed by {} with {} item(s)",
                                savedOrder.getId(), buyer.getUsername(), purchaseItems.size());

        return "redirect:/orders/confirmation?orderId=" + savedOrder.getId();
    }

    @GetMapping("/orders/confirmation")
    @PreAuthorize("hasRole('BUYER')")
    public String confirmation(@RequestParam(defaultValue = "1024") Long orderId,
                               Model model) {
        model.addAttribute("orderId", orderId);
        return "orders/confirmation";
    }

    @GetMapping("/buyer/orders/statuses")
    @PreAuthorize("hasRole('BUYER')")
    @Transactional(readOnly = true)
    @ResponseBody
    public List<Map<String, Object>> buyerOrderStatuses(Principal principal) {
        User buyer = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated buyer not found"));

        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId())
                .stream()
                .map(order -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("orderId", order.getId());
                    payload.put("status", order.getStatus().name());
                    payload.put("canCancel", canBuyerCancel(order.getStatus()));
                    return payload;
                })
                .toList();
    }

    private String buildOrderNotes(String phone, String notes) {
        String safePhone = phone == null ? "" : phone.trim();
        String safeNotes = notes == null ? "" : notes.trim();

        if (!safePhone.isEmpty() && !safeNotes.isEmpty()) {
            return "Phone: " + safePhone + " | Notes: " + safeNotes;
        }
        if (!safePhone.isEmpty()) {
            return "Phone: " + safePhone;
        }
        return safeNotes;
    }

        private CheckoutItemView createCheckoutItem(Product product, int qty) {
                BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
                Integer stock = product.getStock();
                int maxQty = stock == null ? qty : stock;

                return new CheckoutItemView(
                                product.getId(),
                                product.getName(),
                                qty,
                                product.getPrice(),
                                lineTotal,
                                Math.max(1, maxQty)
                );
        }

        private List<CheckoutItemView> buildCheckoutItemsFromCart(Map<Long, Integer> cart) {
                return cart.entrySet().stream()
                                .map(entry -> productRepository.findById(entry.getKey())
                                                .map(product -> {
                                                        Integer stock = product.getStock();
                                                        Integer requested = entry.getValue();
                                                        int requestedQty = requested == null ? 1 : Math.max(1, requested);
                                                        int maxAllowedQty = stock == null ? requestedQty : Math.max(1, stock);
                                                        int finalQty = Math.min(requestedQty, maxAllowedQty);
                                                        return createCheckoutItem(product, finalQty);
                                                })
                                                .orElse(null))
                                .filter(item -> item != null)
                                .toList();
        }

        private BigDecimal calculateSubtotal(List<CheckoutItemView> checkoutItems) {
                return checkoutItems.stream()
                                .map(CheckoutItemView::lineTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal calculateShipping(BigDecimal subtotal) {
                return subtotal.compareTo(BigDecimal.ZERO) > 0
                                ? new BigDecimal("5.00")
                                : BigDecimal.ZERO;
        }

        @SuppressWarnings("unchecked")
        private Map<Long, Integer> getOrCreateCart(HttpSession session) {
                Object existingCart = session.getAttribute(CART_SESSION_KEY);
                if (existingCart instanceof Map<?, ?>) {
                        return (Map<Long, Integer>) existingCart;
                }

                Map<Long, Integer> newCart = new LinkedHashMap<>();
                session.setAttribute(CART_SESSION_KEY, newCart);
                return newCart;
        }

        private boolean canBuyerCancel(Order.OrderStatus status) {
                return status == Order.OrderStatus.PENDING
                                || status == Order.OrderStatus.CONFIRMED
                                || status == Order.OrderStatus.PROCESSING;
        }

        private List<Order.OrderStatus> getSellerUpdatableStatuses() {
                return List.of(
                                Order.OrderStatus.CONFIRMED,
                                Order.OrderStatus.PROCESSING,
                                Order.OrderStatus.SHIPPED,
                                Order.OrderStatus.DELIVERED
                );
        }

        private String normalizeSellerRedirect(String redirectPath) {
                if (redirectPath == null || !redirectPath.startsWith("/seller/")) {
                        return "/seller/orders";
                }
                return redirectPath;
        }

        private record BuyerOrderView(Long id, Integer items, BigDecimal total, Order.OrderStatus status, boolean canCancel) { }

    private record SellerOrderView(Long id, String customer, Integer items, BigDecimal total, Order.OrderStatus status) { }

        private record CheckoutItemView(Long productId, String name, Integer qty, BigDecimal price, BigDecimal lineTotal, Integer maxQty) { }

        private record PurchaseItem(Product product, Integer qty) { }
}
