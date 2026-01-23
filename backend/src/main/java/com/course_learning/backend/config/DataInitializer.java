package com.course_learning.backend.config;

import com.course_learning.backend.model.User;
import com.course_learning.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Value("${app.data.initialize:true}")
    private boolean initializeData;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        if (!initializeData) {
            System.out.println("Data initialization is disabled");
            return;
        }

        // Create or update default admin user
        User adminUser = userRepository.findByUserName("vijayk");
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setUserName("vijayk");
            adminUser.setEmail("vijayk@example.com");
            adminUser.setFirstName("Vijay");
            adminUser.setLastName("Kumar");
            adminUser.setRole("admin");
            adminUser.setActive(true);
            adminUser.setVerified(true);
            System.out.println("Creating default admin user: vijayk");
        } else {
            System.out.println("Updating existing admin user: vijayk");
        }

        // Always set the password to ensure it's BCrypt encoded
        adminUser.setPassword(passwordEncoder.encode("Admin@123"));
        userRepository.save(adminUser);
        System.out.println("Admin user ready: vijayk / Admin@123");

        // Create or update regular test user
        User testUser = userRepository.findByUserName("testuser");
        if (testUser == null) {
            testUser = new User();
            testUser.setUserName("testuser");
            testUser.setEmail("test@example.com");
            testUser.setFirstName("Test");
            testUser.setLastName("User");
            testUser.setRole("user");
            testUser.setActive(true);
            testUser.setVerified(true);
            System.out.println("Creating default test user: testuser");
        } else {
            System.out.println("Updating existing test user: testuser");
        }

        // Always set the password to ensure it's BCrypt encoded
        testUser.setPassword(passwordEncoder.encode("password123"));
        userRepository.save(testUser);
        System.out.println("Test user ready: testuser / password123");
    }
}
