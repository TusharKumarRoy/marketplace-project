package com.lab.marketplace.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.lab.marketplace.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/buyer")
@RequiredArgsConstructor
public class BuyerWebController {

    private final ProductService productService;
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        log.info("Buyer dashboard accessed by: {}", authentication.getName());
        model.addAttribute("username", authentication.getName());

        var featuredProducts = productService.getAllProducts()
                .stream()
                .filter(product -> product.getStock() != null && product.getStock() > 0)
                .limit(6)
                .toList();

        model.addAttribute("featuredProducts", featuredProducts);
        return "buyer/dashboard";
    }
}
