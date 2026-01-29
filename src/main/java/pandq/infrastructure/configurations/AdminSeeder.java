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

        String firebaseUid = null;

        try {
            // 1. Check if admin exists in Firebase
            try {
                com.google.firebase.auth.UserRecord userRecord = com.google.firebase.auth.FirebaseAuth.getInstance().getUserByEmail(adminEmail);
                firebaseUid = userRecord.getUid();
                log.info("Admin exists in Firebase: {}", firebaseUid);
            } catch (com.google.firebase.auth.FirebaseAuthException e) {
                // Check for 'user-not-found' code OR if message indicates user not found
                // The error code might be 'user-not-found' or 'AUTH_USER_NOT_FOUND' depending on SDK version
                if ("user-not-found".equals(e.getErrorCode()) || 
                    (e.getMessage() != null && e.getMessage().contains("No user record found"))) {
                    
                    // 2. Create in Firebase if not found
                    log.info("Admin not found in Firebase (Code: {}), creating...", e.getErrorCode());
                    String tempPassword = generateRandomPassword();
                    
                    com.google.firebase.auth.UserRecord.CreateRequest request = new com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(adminEmail)
                        .setPassword(tempPassword)
                        .setDisplayName(adminName)
                        .setEmailVerified(false);
                    
                    com.google.firebase.auth.UserRecord newUser = com.google.firebase.auth.FirebaseAuth.getInstance().createUser(request);
                    firebaseUid = newUser.getUid();
                    log.info("Created Firebase user for Admin: {}", firebaseUid);

                    // 3. Generate and Log Password Reset Link
                    try {
                        String resetLink = com.google.firebase.auth.FirebaseAuth.getInstance().generatePasswordResetLink(adminEmail);
                        log.warn("\n\n" +
                                "========================================================================================\n" +
                                "🔐 ADMIN PASSWORD RESET LINK GENERATED\n" +
                                "----------------------------------------------------------------------------------------\n" +
                                "Email: {}\n" +
                                "Link : {}\n" +
                                "----------------------------------------------------------------------------------------\n" +
                                "Please click the link above to set your password and verify your email.\n" +
                                "========================================================================================\n", 
                                adminEmail, resetLink);
                    } catch (com.google.firebase.auth.FirebaseAuthException resetEx) {
                        log.warn("Could not generate password reset link for {}: {}", adminEmail, resetEx.getMessage());
                    }

                } else {
                    log.error("Error fetching user by email. Code: {}, Message: {}", e.getErrorCode(), e.getMessage());
                    throw e; // Rethrow other errors
                }
            }
        } catch (Exception e) {
            log.error("Failed to check/create Admin in Firebase: {}", e.getMessage());
            // Proceed to check local DB, as per user requirement (just check/create if needed)
        }

        // 4. Check if admin already exists in Local DB
        if (userRepository.findByEmail(adminEmail).isPresent()) {
            log.info("Admin user already exists in Local DB: {}", adminEmail);
             // Ensure Firebase UID is linked if it wasn't before
            if (firebaseUid != null) {
                 User existingUser = userRepository.findByEmail(adminEmail).get();
                 if (existingUser.getFirebaseUid() == null) {
                     existingUser.setFirebaseUid(firebaseUid);
                     userRepository.save(existingUser);
                     log.info("Linked existing local Admin to Firebase UID");
                 }
            }
            return;
        }

        // 5. Create admin user in Local DB
        LocalDateTime now = LocalDateTime.now();
        User admin = User.builder()
                .email(adminEmail)
                .fullName(adminName)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .firebaseUid(firebaseUid)
                .createdAt(now)
                .updatedAt(now)
                .build();

        userRepository.save(admin);
        log.info("✅ Seeded admin user in Local DB: {} ({})", adminName, adminEmail);
    }

    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
