package com.lab.marketplace.config;

import com.lab.marketplace.entity.Product;
import com.lab.marketplace.entity.Role;
import com.lab.marketplace.entity.User;
import com.lab.marketplace.repository.ProductRepository;
import com.lab.marketplace.repository.RoleRepository;
import com.lab.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
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
    private final ProductRepository productRepository;
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
        createSellerUserIfNotExists();
        seedStarterProductsIfEmpty();

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
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("changeMeAdmin"))
                    .fullName("System Administrator")
                    .phoneNumber("000-000-0000")
                    .enabled(true)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();

            userRepository.save(admin);
            log.info("Created admin user - Username: {}", adminUsername);
            log.warn("WARNING: Default admin credentials are for development only. Configure a secure admin user in production.");
        } else {
            log.info("Admin user already exists");
        }
    }

        private void createSellerUserIfNotExists() {
        String sellerUsername = "seller";

        if (!userRepository.existsByUsername(sellerUsername)) {
            Role sellerRole = roleRepository.findByName(Role.SELLER)
                .orElseThrow(() -> new RuntimeException("SELLER role not found"));

            User seller = User.builder()
                .username(sellerUsername)
                .email("seller@example.com")
                .password(passwordEncoder.encode("changeMeSeller"))
                .fullName("Default Seller")
                .phoneNumber("000-111-2222")
                .enabled(true)
                .roles(new HashSet<>(Set.of(sellerRole)))
                .build();

            userRepository.save(seller);
            log.info("Created default seller user - Username: {}", sellerUsername);
        } else {
            log.info("Seller user already exists");
        }
        }

        private void seedStarterProductsIfEmpty() {
            long inStockProducts = productRepository.findInStockProducts().size();
            if (inStockProducts >= 3) {
                log.info("Sufficient in-stock products already exist ({}); skipping starter seed", inStockProducts);
            return;
        }

        User seller = userRepository.findByUsername("seller")
            .orElseThrow(() -> new RuntimeException("Default seller not found"));

        List<Product> starterProducts = List.of(
            Product.builder()
                .name("Wireless Keyboard")
                .description("Compact wireless keyboard with quiet keys and long battery life.")
                .price(new BigDecimal("29.99"))
                .stock(18)
                .category("Electronics")
                .imageUrl("https://images.unsplash.com/photo-1587829741301-dc798b83add3")
                .available(true)
                .seller(seller)
                .build(),
            Product.builder()
                .name("Mechanical Mouse")
                .description("Ergonomic mouse with adjustable DPI and programmable buttons.")
                .price(new BigDecimal("24.50"))
                .stock(25)
                .category("Electronics")
                .imageUrl("https://images.unsplash.com/photo-1527814050087-3793815479db")
                .available(true)
                .seller(seller)
                .build(),
            Product.builder()
                .name("Notebook Set")
                .description("Premium ruled notebook set for study and office notes.")
                .price(new BigDecimal("12.00"))
                .stock(40)
                .category("Stationery")
                .imageUrl("https://images.unsplash.com/photo-1531346878377-a5be20888e57")
                .available(true)
                .seller(seller)
                .build(),
            Product.builder()
                .name("Insulated Water Bottle")
                .description("Stainless steel bottle keeps drinks cold for 24 hours.")
                .price(new BigDecimal("17.75"))
                .stock(22)
                .category("Lifestyle")
                .imageUrl("https://images.unsplash.com/photo-1602143407151-7111542de6e8")
                .available(true)
                .seller(seller)
                .build(),
            Product.builder()
                .name("Desk Lamp")
                .description("LED desk lamp with brightness control and USB charging port.")
                .price(new BigDecimal("21.99"))
                .stock(14)
                .category("Home")
                .imageUrl("https://images.unsplash.com/photo-1507473885765-e6ed057f782c")
                .available(true)
                .seller(seller)
                .build(),
            Product.builder()
                .name("Canvas Backpack")
                .description("Durable backpack with multiple compartments for daily use.")
                .price(new BigDecimal("34.20"))
                .stock(16)
                .category("Fashion")
                .imageUrl("https://images.unsplash.com/photo-1491637639811-60e2756cc1c7")
                .available(true)
                .seller(seller)
                .build()
        );

        productRepository.saveAll(starterProducts);
        log.info("Seeded {} starter products to improve buyer experience", starterProducts.size());
        }
}
