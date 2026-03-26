package com.lab.marketplace.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.lab.marketplace.dto.ProductRequest;
import com.lab.marketplace.dto.ProductResponse;
import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.exception.UnauthorizedException;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProductSavesWhenUserHasSellerRole() {
        ProductRequest request = ProductRequest.builder()
                .name("Gaming Mouse")
                .description("High precision wireless gaming mouse")
                .price(new BigDecimal("49.99"))
                .stock(25)
                .category("Accessories")
                .imageUrl("img.png")
                .build();

        User seller = User.builder().id(10L).username("seller1").build();
        seller.addRole(Role.builder().name(Role.SELLER).build());

        Product saved = Product.builder()
                .id(100L)
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .seller(seller)
                .build();

        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse response = productService.createProduct(request, "seller1");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getSellerId()).isEqualTo(10L);
        assertThat(response.getSellerUsername()).isEqualTo("seller1");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProductThrowsWhenUserIsNotSeller() {
        ProductRequest request = ProductRequest.builder()
                .name("Desk")
                .description("Solid wood office desk")
                .price(new BigDecimal("199.99"))
                .stock(3)
                .category("Furniture")
                .build();

        User buyer = User.builder().username("buyer1").build();
        buyer.addRole(Role.builder().name(Role.BUYER).build());

        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> productService.createProduct(request, "buyer1"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Only sellers can create products");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductThrowsWhenUserIsNotOwner() {
        User owner = User.builder().username("seller-owner").build();
        Product existing = Product.builder().id(7L).seller(owner).build();

        ProductRequest request = ProductRequest.builder()
                .name("Updated")
                .description("Updated description")
                .price(new BigDecimal("9.99"))
                .stock(1)
                .category("Test")
                .build();

        when(productRepository.findById(7L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> productService.updateProduct(7L, request, "other-user"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("You can only update your own products");
    }

    @Test
    void deleteProductDeletesWhenUserOwnsProduct() {
        User owner = User.builder().username("seller-owner").build();
        Product existing = Product.builder().id(8L).name("Keyboard").seller(owner).build();
        when(productRepository.findById(8L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(8L, "seller-owner");

        verify(productRepository).delete(existing);
    }

    @Test
    void getProductsBySellerReturnsMappedResponses() {
        User seller = User.builder().id(42L).username("seller42").build();
        Product p1 = Product.builder().id(1L).name("P1").price(new BigDecimal("1.00")).stock(1).seller(seller).build();
        Product p2 = Product.builder().id(2L).name("P2").price(new BigDecimal("2.00")).stock(2).seller(seller).build();

        when(userRepository.findByUsername("seller42")).thenReturn(Optional.of(seller));
        when(productRepository.findBySellerId(42L)).thenReturn(List.of(p1, p2));

        List<ProductResponse> responses = productService.getProductsBySeller("seller42");

        assertThat(responses).hasSize(2);
        assertThat(responses).extracting(ProductResponse::getName).containsExactly("P1", "P2");
    }
}

