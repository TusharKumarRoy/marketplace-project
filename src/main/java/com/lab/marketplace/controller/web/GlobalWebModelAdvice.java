package com.lab.marketplace.controller.web;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Map;

@ControllerAdvice
public class GlobalWebModelAdvice {

    private static final String CART_SESSION_KEY = "MARKETPLACE_CART";

    @ModelAttribute("cartCount")
    public int cartCount(HttpSession session) {
        Object cartObj = session.getAttribute(CART_SESSION_KEY);
        if (!(cartObj instanceof Map<?, ?> cartMap)) {
            return 0;
        }

        return cartMap.values().stream()
                .filter(Integer.class::isInstance)
                .mapToInt(value -> (Integer) value)
                .sum();
    }
}
