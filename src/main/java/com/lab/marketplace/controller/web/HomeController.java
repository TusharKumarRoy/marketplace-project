package com.lab.marketplace.controller.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class HomeController {
    
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        
        // Redirect based on user role
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            log.info("Redirecting admin user to dashboard");
            return "redirect:/admin/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SELLER"))) {
            log.info("Redirecting seller user to seller dashboard");
            return "redirect:/seller/dashboard";
        } else if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_BUYER"))) {
            log.info("Redirecting buyer user to buyer dashboard");
            return "redirect:/buyer/dashboard";
        }
        
        return "redirect:/login";
    }
    
    @GetMapping("/home")
    public String homeAlias(Authentication authentication) {
        return home(authentication);
    }
}
