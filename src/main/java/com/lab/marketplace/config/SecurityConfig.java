package com.lab.marketplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Password encoder using BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Authentication manager
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security filter chain - URL-based authorization
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**") // CSRF disabled for stateless REST API endpoints
            )
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers("/", "/home", "/login", "/register").permitAll()
                .requestMatchers("/products", "/products/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/images/**", "/static/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                .requestMatchers("/api/products", "/api/products/**").permitAll() // Public product browsing
                
                // Admin endpoints
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Seller endpoints - product management
                .requestMatchers("/seller/**").hasRole("SELLER")
                
                // Buyer endpoints
                .requestMatchers("/buyer/**").hasRole("BUYER")
                .requestMatchers("/cart", "/cart/**").hasRole("BUYER")
                .requestMatchers("/orders/checkout", "/orders/place", "/orders/confirmation").hasRole("BUYER")
                
                // API endpoints with role-based access
                .requestMatchers("/api/auth/me").authenticated()
                
                // All other requests need authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )
            .exceptionHandling(exception -> exception
                .accessDeniedPage("/login?error=access-denied")
            );

        return http.build();
    }
}
