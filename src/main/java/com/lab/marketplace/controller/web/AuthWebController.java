package com.lab.marketplace.controller.web;

import com.lab.marketplace.dto.RegisterRequest;
import com.lab.marketplace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthWebController {
    
    private final UserService userService;
    
    @GetMapping("/login")
    public String loginPage(@ModelAttribute("error") String error, Model model) {
        if (error != null && !error.isEmpty()) {
            model.addAttribute("error", error);
        }
        return "auth/login";
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        log.info("Web: Register request for username: {}", request.getUsername());
        
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        
        try {
            userService.register(request);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Registration failed: ", e);
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }
}
