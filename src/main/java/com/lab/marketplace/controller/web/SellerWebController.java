package com.lab.marketplace.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/seller")
@RequiredArgsConstructor
public class SellerWebController {
    
    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        log.info("Seller dashboard accessed by: {}", authentication.getName());
        model.addAttribute("username", authentication.getName());
        return "seller/dashboard";
    }
}
