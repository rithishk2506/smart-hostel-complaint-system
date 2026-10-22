package com.hostelcomplaint.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.hostelcomplaint.entity.AppUser;
import com.hostelcomplaint.repository.AppUserRepository;

@Component
public class InitialAdminInitializer implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(InitialAdminInitializer.class);

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.initial-admin.username:}")
    private String adminUsername;

    @Value("${app.initial-admin.password:}")
    private String adminPassword;

    public InitialAdminInitializer(
            AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {

        if (adminUsername == null || adminUsername.isBlank()
                || adminPassword == null || adminPassword.length() < 12) {
            log.warn("Initial admin not created. Configure "
                    + "app.initial-admin.username and a password of at least "
                    + "12 characters in application.properties.");
            return;
        }

        String username = adminUsername.trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            log.info("Configured initial-admin account already exists.");
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(username);
        admin.setFullName("System Administrator");
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setEnabled(true);

        userRepository.save(admin);
        log.info("Initial administrator account created: {}", username);
    }
}