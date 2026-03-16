package com.lab.marketplace;

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
class OrderServiceTest {

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ProductRepository productRepository;

	@Test
	void findByBuyerIdReturnsOnlyBuyersOrders() {
		User buyerA = createUser("buyerA");
		User buyerB = createUser("buyerB");
		User seller = createUser("sellerA");

		createOrder(buyerA, seller, Order.OrderStatus.PENDING, "10.00", LocalDateTime.now().minusMinutes(2));
		createOrder(buyerB, seller, Order.OrderStatus.PENDING, "12.00", LocalDateTime.now().minusMinutes(1));

		List<Order> buyerOrders = orderRepository.findByBuyerId(buyerA.getId());

		assertThat(buyerOrders).hasSize(1);
		assertThat(buyerOrders.get(0).getBuyer().getId()).isEqualTo(buyerA.getId());
	}

	@Test
	void findByStatusReturnsMatchingStatusOnly() {
		User buyer = createUser("buyerC");
		User seller = createUser("sellerB");

		createOrder(buyer, seller, Order.OrderStatus.SHIPPED, "15.00", LocalDateTime.now().minusMinutes(2));
		createOrder(buyer, seller, Order.OrderStatus.PENDING, "20.00", LocalDateTime.now().minusMinutes(1));

		List<Order> shipped = orderRepository.findByStatus(Order.OrderStatus.SHIPPED);

		assertThat(shipped).hasSize(1);
		assertThat(shipped.get(0).getStatus()).isEqualTo(Order.OrderStatus.SHIPPED);
	}

	@Test
	void findByBuyerIdAndStatusFiltersByBothFields() {
		User buyer = createUser("buyerD");
		User otherBuyer = createUser("buyerE");
		User seller = createUser("sellerC");

		createOrder(buyer, seller, Order.OrderStatus.CONFIRMED, "21.00", LocalDateTime.now().minusMinutes(3));
		createOrder(buyer, seller, Order.OrderStatus.CANCELLED, "22.00", LocalDateTime.now().minusMinutes(2));
		createOrder(otherBuyer, seller, Order.OrderStatus.CONFIRMED, "23.00", LocalDateTime.now().minusMinutes(1));

		List<Order> confirmed = orderRepository.findByBuyerIdAndStatus(buyer.getId(), Order.OrderStatus.CONFIRMED);

		assertThat(confirmed).hasSize(1);
		assertThat(confirmed.get(0).getBuyer().getId()).isEqualTo(buyer.getId());
		assertThat(confirmed.get(0).getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
	}

	@Test
	void findOrdersForSellerExcludesCancelledAndReturnsDistinctOrders() {
		User buyer = createUser("buyerF");
		User seller = createUser("sellerD");
		User otherSeller = createUser("sellerE");

		Order included = createOrder(buyer, seller, Order.OrderStatus.CONFIRMED, "30.00", LocalDateTime.now().minusMinutes(3));
		Product secondSellerProduct = createProduct("SellerD Product 2", seller, "9.00");
		OrderItem secondItem = OrderItem.builder()
				.product(secondSellerProduct)
				.quantity(1)
				.unitPrice(new BigDecimal("9.00"))
				.subtotal(new BigDecimal("9.00"))
				.build();
		included.addOrderItem(secondItem);
		orderRepository.save(included);

		createOrder(buyer, seller, Order.OrderStatus.CANCELLED, "31.00", LocalDateTime.now().minusMinutes(2));
		createOrder(buyer, otherSeller, Order.OrderStatus.CONFIRMED, "32.00", LocalDateTime.now().minusMinutes(1));

		List<Order> sellerOrders = orderRepository.findOrdersForSeller(seller.getId(), Order.OrderStatus.CANCELLED);

		assertThat(sellerOrders).hasSize(1);
		assertThat(sellerOrders.get(0).getId()).isEqualTo(included.getId());
		assertThat(sellerOrders.get(0).getStatus()).isNotEqualTo(Order.OrderStatus.CANCELLED);
	}

	@Test
	void findByBuyerIdOrderByCreatedAtDescReturnsNewestFirst() {
		User buyer = createUser("buyerG");
		User seller = createUser("sellerF");

		Order older = createOrder(buyer, seller, Order.OrderStatus.PENDING, "40.00", LocalDateTime.now().minusDays(1));
		Order newer = createOrder(buyer, seller, Order.OrderStatus.PENDING, "41.00", LocalDateTime.now());

		List<Order> ordered = orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyer.getId());

		assertThat(ordered).hasSize(2);
		assertThat(ordered.get(0).getId()).isEqualTo(newer.getId());
		assertThat(ordered.get(1).getId()).isEqualTo(older.getId());
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

	private Product createProduct(String name, User seller, String price) {
		Product product = Product.builder()
				.name(name)
				.description("desc")
				.price(new BigDecimal(price))
				.stock(10)
				.category("test")
				.seller(seller)
				.available(true)
				.build();
		return productRepository.save(product);
	}

	private Order createOrder(User buyer, User seller, Order.OrderStatus status, String amount, LocalDateTime createdAt) {
		Product product = createProduct("Product-" + amount + "-" + buyer.getUsername(), seller, amount);

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

