package com.lab.marketplace.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.UserRepository;

@DataJpaTest
class ProductIntegrationTest {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void findInStockProductsReturnsOnlyAvailableProductsWithPositiveStock() {
		User seller = createUser("sellerA");

		createProduct("Keyboard", seller, "40.00", true, 5, "Accessories");
		createProduct("Mouse", seller, "20.00", false, 7, "Accessories");
		createProduct("Monitor", seller, "150.00", true, 0, "Electronics");

		List<Product> inStock = productRepository.findInStockProducts();

		assertThat(inStock).hasSize(1);
		assertThat(inStock.get(0).getName()).isEqualTo("Keyboard");
	}

	@Test
	void findByNameContainingIgnoreCaseAndCategoryReturnExpectedData() {
		User seller = createUser("sellerB");

		createProduct("Gaming Chair", seller, "120.00", true, 4, "Furniture");
		createProduct("Office Chair", seller, "90.00", true, 6, "Furniture");
		createProduct("Desk Lamp", seller, "30.00", true, 12, "Lighting");

		List<Product> chairs = productRepository.findByNameContainingIgnoreCase("chair");
		List<Product> furniture = productRepository.findByCategory("Furniture");

		assertThat(chairs).extracting(Product::getName)
				.containsExactlyInAnyOrder("Gaming Chair", "Office Chair");
		assertThat(furniture).extracting(Product::getCategory)
				.containsOnly("Furniture");
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

	private Product createProduct(String name, User seller, String price, boolean available, int stock, String category) {
		Product product = Product.builder()
				.name(name)
				.description("desc")
				.price(new BigDecimal(price))
				.stock(stock)
				.category(category)
				.seller(seller)
				.available(available)
				.build();
		return productRepository.save(product);
	}
}

