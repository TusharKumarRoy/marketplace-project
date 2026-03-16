package com.lab.marketplace.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.lab.marketplace.entity.Order;
import com.lab.marketplace.entity.OrderItem;
import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.OrderRepository;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.UserRepository;

@DataJpaTest
class OrderIntegrationTest {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void findByBuyerIdAndStatusReturnsOnlyMatchingOrders() {
		User buyer = createUser("buyerA");
		User seller = createUser("sellerA");

		createOrder(buyer, seller, Order.OrderStatus.CONFIRMED, "15.00", LocalDateTime.now().minusMinutes(5));
		createOrder(buyer, seller, Order.OrderStatus.CANCELLED, "25.00", LocalDateTime.now().minusMinutes(1));

		List<Order> orders = orderRepository.findByBuyerIdAndStatus(buyer.getId(), Order.OrderStatus.CONFIRMED);

		assertThat(orders).hasSize(1);
		assertThat(orders.get(0).getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
		assertThat(orders.get(0).getBuyer().getId()).isEqualTo(buyer.getId());
	}

	@Test
	void findOrdersForSellerExcludesCancelledAndReturnsDistinctOrder() {
		User buyer = createUser("buyerB");
		User seller = createUser("sellerB");

		Order included = createOrder(buyer, seller, Order.OrderStatus.PROCESSING, "30.00", LocalDateTime.now().minusMinutes(2));
		Product extraProduct = createProduct("Extra Product", seller, "5.00", true, 7);
		OrderItem extraItem = OrderItem.builder()
				.product(extraProduct)
				.quantity(2)
				.unitPrice(new BigDecimal("5.00"))
				.subtotal(new BigDecimal("10.00"))
				.build();
		included.addOrderItem(extraItem);
		orderRepository.save(included);

		createOrder(buyer, seller, Order.OrderStatus.CANCELLED, "20.00", LocalDateTime.now().minusMinutes(1));

		List<Order> results = orderRepository.findOrdersForSeller(seller.getId(), Order.OrderStatus.CANCELLED);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getId()).isEqualTo(included.getId());
		assertThat(results.get(0).getStatus()).isNotEqualTo(Order.OrderStatus.CANCELLED);
	}

	private User createUser(String username) {
		User user = User.builder()
				.username(username)
				.email(username + "@test.com")
				.password("pw")
				.fullName("Test " + username)
				.enabled(true)
				.build();
		return userRepository.save(user);
	}

	private Product createProduct(String name, User seller, String price, boolean available, int stock) {
		Product product = Product.builder()
				.name(name)
				.description("desc")
				.price(new BigDecimal(price))
				.stock(stock)
				.category("test")
				.seller(seller)
				.available(available)
				.build();
		return productRepository.save(product);
	}

	private Order createOrder(User buyer, User seller, Order.OrderStatus status, String amount, LocalDateTime createdAt) {
		Product product = createProduct("Product-" + amount, seller, amount, true, 10);
		BigDecimal total = new BigDecimal(amount);

		OrderItem item = OrderItem.builder()
				.product(product)
				.quantity(1)
				.unitPrice(total)
				.subtotal(total)
				.build();

		Order order = Order.builder()
				.buyer(buyer)
				.status(status)
				.totalAmount(total)
				.shippingAddress("Test Address")
				.notes("Test Note")
				.build();

		order.addOrderItem(item);
		order.setCreatedAt(createdAt);
		return orderRepository.save(order);
	}
}

