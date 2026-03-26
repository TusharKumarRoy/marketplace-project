package com.lab.marketplace.controller.web;

import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lab.marketplace.dto.ProductRequest;
import com.lab.marketplace.dto.ProductResponse;
import com.lab.marketplace.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping
@RequiredArgsConstructor
public class ProductWebController {

    private final ProductService productService;

    @GetMapping("/products")
    public String listProducts(@RequestParam(required = false) String category,
                               @RequestParam(required = false) String q,
                               Model model) {
        List<ProductResponse> products;

        if (q != null && !q.isBlank()) {
            products = productService.searchProducts(q.trim());
        } else if (category != null && !category.isBlank()) {
            products = productService.getProductsByCategory(category.trim());
        } else {
            products = productService.getAllProducts();
        }

        Set<String> categories = productService.getAllProducts()
                .stream()
                .map(ProductResponse::getCategory)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(TreeSet::new));

        model.addAttribute("products", products);
        model.addAttribute("categories", categories);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("searchQuery", q);
        return "products/list";
    }

    @GetMapping("/products/search")
    public String searchProducts(@RequestParam(required = false) String q, Model model) {
        List<ProductResponse> products = (q == null || q.isBlank())
                ? productService.getAllProducts()
                : productService.searchProducts(q.trim());

        model.addAttribute("products", products);
        model.addAttribute("searchQuery", q == null ? "" : q);
        return "products/search";
    }

    @GetMapping("/products/{id}")
    public String productDetails(@PathVariable Long id, Model model) {
        model.addAttribute("product", productService.getProductById(id));
        return "products/details";
    }

    @GetMapping("/seller/products")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerProducts(Principal principal, Model model) {
        log.info("Rendering seller products for user: {}", principal.getName());
        model.addAttribute("products", productService.getProductsBySeller(principal.getName()));
        return "seller/products";
    }

    @GetMapping("/seller/products/new")
    @PreAuthorize("hasRole('SELLER')")
    public String newProductForm(Model model) {
        model.addAttribute("product", ProductRequest.builder().build());
        model.addAttribute("isEdit", false);
        model.addAttribute("formAction", "/seller/products");
        return "seller/product-form";
    }

    @GetMapping("/seller/products/{id}/edit")
    @PreAuthorize("hasRole('SELLER')")
    public String editProductForm(@PathVariable Long id,
                                  Principal principal,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        ProductResponse existing = productService.getProductById(id);
        if (!principal.getName().equals(existing.getSellerUsername())) {
            redirectAttributes.addFlashAttribute("error", "You can only edit your own products.");
            return "redirect:/seller/products";
        }

        ProductRequest request = ProductRequest.builder()
                .name(existing.getName())
                .description(existing.getDescription())
                .price(existing.getPrice())
                .stock(existing.getStock())
                .category(existing.getCategory())
                .imageUrl(existing.getImageUrl())
                .build();

        model.addAttribute("product", request);
        model.addAttribute("productId", id);
        model.addAttribute("isEdit", true);
        model.addAttribute("formAction", "/seller/products/" + id);
        model.addAttribute("ownerUsername", principal.getName());
        return "seller/product-form";
    }

    @PostMapping("/seller/products")
    @PreAuthorize("hasRole('SELLER')")
    public String createProduct(@Valid @ModelAttribute("product") ProductRequest product,
                                BindingResult bindingResult,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            model.addAttribute("formAction", "/seller/products");
            return "seller/product-form";
        }

        try {
            productService.createProduct(product, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Product created successfully. It is now visible to buyers.");
            return "redirect:/seller/products";
        } catch (RuntimeException ex) {
            model.addAttribute("isEdit", false);
            model.addAttribute("formAction", "/seller/products");
            model.addAttribute("submitError", ex.getMessage());
            return "seller/product-form";
        }
    }

    @PostMapping("/seller/products/{id}")
    @PreAuthorize("hasRole('SELLER')")
    public String updateProduct(@PathVariable Long id,
                                @Valid @ModelAttribute("product") ProductRequest product,
                                BindingResult bindingResult,
                                Principal principal,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            model.addAttribute("formAction", "/seller/products/" + id);
            return "seller/product-form";
        }

        try {
            productService.updateProduct(id, product, principal.getName());
            redirectAttributes.addFlashAttribute("success", "Product updated successfully.");
            return "redirect:/seller/products";
        } catch (RuntimeException ex) {
            model.addAttribute("isEdit", true);
            model.addAttribute("productId", id);
            model.addAttribute("formAction", "/seller/products/" + id);
            model.addAttribute("submitError", ex.getMessage());
            return "seller/product-form";
        }
    }

    @GetMapping("/seller/products/search")
    @PreAuthorize("hasRole('SELLER')")
    public String sellerSearch(@RequestParam(required = false) String q,
                               Principal principal,
                               Model model) {
        List<ProductResponse> mine = productService.getProductsBySeller(principal.getName());
        List<ProductResponse> filtered = (q == null || q.isBlank())
                ? mine
                : mine.stream()
                .filter(product -> product.getName() != null
                        && product.getName().toLowerCase().contains(q.trim().toLowerCase()))
                .toList();

        model.addAttribute("products", filtered);
        model.addAttribute("searchQuery", q == null ? "" : q);
        return "seller/products";
    }

    @GetMapping("/products/details-preview")
    public String detailsPreview(@RequestParam(required = false) Long id,
                                 Authentication authentication,
                                 Model model) {
        if (id != null) {
            return "redirect:/products/" + id;
        }

        List<ProductResponse> products = productService.getAllProducts();
        if (!products.isEmpty()) {
            return "redirect:/products/" + products.get(0).getId();
        }

        model.addAttribute("product", null);
        model.addAttribute("authenticated", authentication != null && authentication.isAuthenticated());
        return "products/details";
    }
}
