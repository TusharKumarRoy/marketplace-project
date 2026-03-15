package com.lab.marketplace.service;

import com.lab.marketplace.dto.ProductRequest;
import com.lab.marketplace.dto.ProductResponse;
import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.exception.ResourceNotFoundException;
import com.lab.marketplace.exception.UnauthorizedException;
import com.lab.marketplace.repository.ProductRepository;
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
public class ProductService {
    
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public ProductResponse createProduct(ProductRequest request, String username) {
        log.info("Creating product: {} by user: {}", request.getName(), username);
        
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        // Verify user has SELLER role
        if (!seller.hasRole("SELLER")) {
            throw new UnauthorizedException("Only sellers can create products");
        }
        
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .seller(seller)
                .build();
        
        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully: {}", savedProduct.getName());
        
        return ProductResponse.fromEntity(savedProduct);
    }
    
    @Transactional
    public ProductResponse updateProduct(Long productId, ProductRequest request, String username) {
        log.info("Updating product ID: {} by user: {}", productId, username);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Verify ownership
        if (!product.getSeller().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only update your own products");
        }
        
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setImageUrl(request.getImageUrl());
        
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getName());
        
        return ProductResponse.fromEntity(updatedProduct);
    }
    
    @Transactional
    public void deleteProduct(Long productId, String username) {
        log.info("Deleting product ID: {} by user: {}", productId, username);
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        
        // Verify ownership
        if (!product.getSeller().getUsername().equals(username)) {
            throw new UnauthorizedException("You can only delete your own products");
        }
        
        productRepository.delete(product);
        log.info("Product deleted successfully: {}", product.getName());
    }
    
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        return ProductResponse.fromEntity(product);
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsBySeller(String username) {
        User seller = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        
        return productRepository.findBySellerId(seller.getId()).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword)
                .stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ProductResponse> getProductsByCategory(String category) {
        return productRepository.findByCategory(category).stream()
                .map(ProductResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<String> getDistinctCategories() {
        return productRepository.findDistinctCategories();
    }
}
