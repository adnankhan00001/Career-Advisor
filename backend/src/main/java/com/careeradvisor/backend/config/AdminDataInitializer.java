package com.careeradvisor.backend.config;

import com.careeradvisor.backend.model.Role;
import com.careeradvisor.backend.model.User;
import com.careeradvisor.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AdminDataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@careeradvisor.dev}")
    private String adminEmail;

    @Value("${app.admin.password:AdminPass123!}")
    private String adminPassword;

    @Value("${app.admin.name:System Administrator}")
    private String adminName;

    @Override
    public void run(String... args) {
        if (!org.springframework.util.StringUtils.hasText(adminEmail) || !org.springframework.util.StringUtils.hasText(adminPassword)) {
            logger.info("Administrator auto-bootstrap skipped (no configured admin credentials).");
            return;
        }

        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (!userRepository.existsByEmail(normalizedEmail)) {
            User admin = User.builder()
                    .name(adminName.trim())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ADMIN)
                    .careerGoal("Engineering Leadership & Architecture")
                    .userLevel("Advanced")
                    .build();

            userRepository.save(admin);
            logger.info("Administrator account initialized successfully for email: {}", normalizedEmail);
        } else {
            // Ensure existing admin account retains ADMIN role
            userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
                if (user.getRole() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    userRepository.save(user);
                    logger.info("Updated role to ADMIN for configured admin account: {}", normalizedEmail);
                }
            });
        }
    }
}
