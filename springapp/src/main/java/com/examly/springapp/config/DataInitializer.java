package com.examly.springapp.config;

import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Environment environment;

    @Value("${ADMIN_INITIAL_USERNAME:admin}")
    private String initialAdminUsername;

    @Value("${ADMIN_INITIAL_PASSWORD:}")
    private String initialAdminPassword;

    @Value("${app.bootstrap.dev-accounts:false}")
    private boolean devAccountsEnabled;

    @Override
    public void run(String... args) {
        if (isProductionEnvironment()) {
            handleProductionAdminProvisioning();
        } else if (devAccountsEnabled) {
            handleDevelopmentSeed();
        } else {
            logger.info("Development account seeding disabled (app.bootstrap.dev-accounts=false).");
        }
    }

    private boolean isProductionEnvironment() {
        if (environment == null || environment.getActiveProfiles() == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equalsIgnoreCase("prod") || p.equalsIgnoreCase("production"));
    }

    private void handleProductionAdminProvisioning() {
        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() != null && "admin".equalsIgnoreCase(u.getRole()))
                .count();

        if (adminCount > 0) {
            logger.info("Production admin account verification complete. Existing administrator accounts preserved.");
            return;
        }

        if (initialAdminPassword != null && !initialAdminPassword.isBlank()) {
            if (initialAdminPassword.length() < 8) {
                logger.error("ADMIN_INITIAL_PASSWORD does not meet minimum password length (>= 8 characters). Bootstrap skipped.");
                return;
            }
            User admin = new User();
            admin.setUsername(initialAdminUsername);
            admin.setPassword(passwordEncoder.encode(initialAdminPassword));
            admin.setRole("admin");
            admin.setLeaveBalance(20);
            userRepository.save(admin);
            logger.info("Secure initial administrator '{}' provisioned successfully from deployment environment.", initialAdminUsername);
        } else {
            logger.warn("SECURITY NOTICE: No administrator accounts found in database. To provision an initial admin in production, set ADMIN_INITIAL_PASSWORD in the environment during first startup.");
        }
    }

    private void handleDevelopmentSeed() {
        if (userRepository.findByUsername("admin") == null) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
            admin.setRole("admin");
            admin.setLeaveBalance(20);
            userRepository.save(admin);
            logger.info("Dev seed: Default admin account verified/created for development.");
        }

        if (userRepository.findByUsername("employee1") == null) {
            User employee = new User();
            employee.setUsername("employee1");
            employee.setPassword(passwordEncoder.encode("password123"));
            employee.setRole("employee");
            employee.setLeaveBalance(20);
            userRepository.save(employee);
            logger.info("Dev seed: Default employee account verified/created for development.");
        }
    }
}
