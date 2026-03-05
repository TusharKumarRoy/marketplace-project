package com.lab.marketplace.controller.rest;

import com.lab.marketplace.dto.LoginRequest;
import com.lab.marketplace.dto.RegisterRequest;
import com.lab.marketplace.dto.UserResponse;
import com.lab.marketplace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRestController {
    
    private final UserService userService;
    
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("REST: Register request for username: {}", request.getUsername());
        UserResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("REST: Login request for: {}", request.getUsernameOrEmail());
        UserResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        log.info("REST: Getting current user: {}", principal.getName());
        UserResponse response = userService.getUserByUsername(principal.getName());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/users/{username}")
    public ResponseEntity<UserResponse> getUserByUsername(@PathVariable String username) {
        log.info("REST: Getting user by username: {}", username);
        UserResponse response = userService.getUserByUsername(username);
        return ResponseEntity.ok(response);
    }
}
