package pandq.infrastructure.configurations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
import pandq.domain.models.user.User;

import java.time.LocalDateTime;

/**
 * Seeds the initial admin user on application startup.
 * Configuration is read from environment variables:
 * - ADMIN_SEED_ENABLED: whether to run the seeder (default: true)
 * - ADMIN_SEED_EMAIL: admin email (default: admin@pandq.com)
 * - ADMIN_SEED_NAME: admin display name (default: Super Admin)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;

    @Value("${app.admin.seed.enabled:true}")
    private boolean seedEnabled;

    @Value("${app.admin.seed.email:admin@pandq.com}")
    private String adminEmail;

    @Value("${app.admin.seed.name:Super Admin}")
    private String adminName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("Admin seeding is disabled");
            return;
        }

        // Check if admin already exists
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists: {}", adminEmail);
            return;
        }

        // Create admin user with timestamps
        LocalDateTime now = LocalDateTime.now();
        User admin = User.builder()
                .email(adminEmail)
                .fullName(adminName)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(admin);
        log.info("✅ Seeded admin user: {} ({})", adminName, adminEmail);
    }
}
