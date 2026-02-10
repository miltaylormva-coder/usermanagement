package com.project.usermanagement.config;

import com.project.usermanagement.entity.Role;
import com.project.usermanagement.enums.RoleName;
import com.project.usermanagement.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Initialize database with default roles
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(RoleRepository roleRepository) {
        return args -> {
            logger.info("Initializing database with default roles...");

            // Create ROLE_USER if it doesn't exist
            if (!roleRepository.existsByName(RoleName.ROLE_USER)) {
                Role userRole = new Role(RoleName.ROLE_USER);
                roleRepository.save(userRole);
                logger.info("✅ Created ROLE_USER");
            } else {
                logger.info("ROLE_USER already exists");
            }

            // Create ROLE_ADMIN if it doesn't exist
            if (!roleRepository.existsByName(RoleName.ROLE_ADMIN)) {
                Role adminRole = new Role(RoleName.ROLE_ADMIN);
                roleRepository.save(adminRole);
                logger.info("✅ Created ROLE_ADMIN");
            } else {
                logger.info("ROLE_ADMIN already exists");
            }

            logger.info("Database initialization complete!");
        };
    }
}