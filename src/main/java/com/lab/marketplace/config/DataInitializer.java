package com.lab.marketplace.config;

import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.RoleRepository;
import com.lab.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Data initializer to seed the database with roles and admin user
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        // Create roles if they don't exist
        createRoleIfNotExists(Role.ADMIN, "Administrator with full access");
        createRoleIfNotExists(Role.SELLER, "Seller who can manage products");
        createRoleIfNotExists(Role.BUYER, "Buyer who can purchase products");

        // Create admin user if not exists
        createAdminUserIfNotExists();

        log.info("Data initialization completed successfully!");
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (!roleRepository.existsByName(roleName)) {
            Role role = new Role(roleName, description);
            roleRepository.save(role);
            log.info("Created role: {}", roleName);
        } else {
            log.info("Role already exists: {}", roleName);
        }
    }

    private void createAdminUserIfNotExists() {
        String adminUsername = "admin";
        
        if (!userRepository.existsByUsername(adminUsername)) {
            // Get ADMIN role
            Role adminRole = roleRepository.findByName(Role.ADMIN)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            // Create admin user
            User admin = User.builder()
                    .username(adminUsername)
                    .email("admin@marketplace.com")
                    .password(passwordEncoder.encode("admin123")) // Change in production!
                    .fullName("System Administrator")
                    .phoneNumber("000-000-0000")
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            userRepository.save(admin);
            log.info("Created admin user - Username: {} | Password: admin123", adminUsername);
            log.warn("WARNING: Please change the admin password in production!");
        } else {
            log.info("Admin user already exists");
        }
    }
}
