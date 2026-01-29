package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.UserDTO;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.enums.NotificationType;
import pandq.domain.models.enums.Role;
import pandq.domain.models.enums.UserStatus;
import pandq.domain.models.user.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<UserDTO.Response> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserDTO.Response getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public UserDTO.Response getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return mapToResponse(user);
    }

    @Transactional
    public UserDTO.Response createUser(UserDTO.CreateRequest request) {
        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("User with email " + request.getEmail() + " already exists");
        }
        
        String firebaseUid = null;
        
        // Create Firebase user for ADMIN and STAFF roles
        if (request.getRole() == Role.ADMIN || request.getRole() == Role.STAFF) {
            try {
                // Generate random temporary password
                String tempPassword = generateRandomPassword();
                
                // Create Firebase user with temporary password
                com.google.firebase.auth.UserRecord.CreateRequest firebaseRequest = 
                    new com.google.firebase.auth.UserRecord.CreateRequest()
                        .setEmail(request.getEmail())
                        .setPassword(tempPassword)
                        .setDisplayName(request.getFullName())
                        .setEmailVerified(false); // Not verified, will verify via reset link
                
                com.google.firebase.auth.UserRecord firebaseUser = 
                    com.google.firebase.auth.FirebaseAuth.getInstance().createUser(firebaseRequest);
                firebaseUid = firebaseUser.getUid();
                log.info("Created Firebase user for {}: {}", request.getEmail(), firebaseUid);
                
                // Send password reset email so user can set their own password
                try {
                    String resetLink = com.google.firebase.auth.FirebaseAuth.getInstance()
                        .generatePasswordResetLink(request.getEmail());
                    log.info("Password reset link generated for {}: {}", request.getEmail(), resetLink);
                    // Firebase will automatically send email to user with the reset link
                } catch (com.google.firebase.auth.FirebaseAuthException resetEx) {
                    log.warn("Could not generate password reset link for {}: {}", 
                        request.getEmail(), resetEx.getMessage());
                    // Continue anyway - user was created
                }
                
            } catch (com.google.firebase.auth.FirebaseAuthException e) {
                log.error("Failed to create Firebase user for {}: {}", request.getEmail(), e.getMessage());
                throw new RuntimeException("Failed to create Firebase account: " + e.getMessage());
            }
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .firebaseUid(firebaseUid)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user in database: {} with role {}", request.getEmail(), request.getRole());
        return mapToResponse(savedUser);
    }
    
    /**
     * Generate a random secure password
     */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @Transactional
    public UserDTO.Response updateUser(UUID id, UserDTO.UpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        user.setAvatarUrl(request.getAvatarUrl());

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    private UserDTO.Response mapToResponse(User user) {
        UserDTO.Response response = new UserDTO.Response();
        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFullName(user.getFullName());
        response.setPhone(user.getPhone());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        return response;
    }

    @Transactional
    public void updateFcmToken(UUID userId, String fcmToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public String getFcmToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getFcmToken();
    }


    /**
     * Update FCM token for a user by email.
     * If user doesn't exist in database, create a new user (for Firebase-only users).
     * This implements "upsert" logic for Firebase Auth integration.
     * @param email User's email
     * @param fcmToken FCM token for push notifications
     * @param firebaseUid Firebase UID for linking (optional for existing users)
     */
    @Transactional
    public void updateFcmTokenByEmail(String email, String fcmToken, String firebaseUid) {
        Optional<User> existingUser = userRepository.findByEmail(email);
        
        User user;
        boolean isNewUser = false;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            log.info("Updating FCM token for existing user: {}", email);
            // Update Firebase UID if provided and not already set
            if (firebaseUid != null && user.getFirebaseUid() == null) {
                user.setFirebaseUid(firebaseUid);
                log.info("Linked Firebase UID for existing user: {}", email);
            }
        } else {
            // Auto-create user from Firebase Auth
            log.info("Creating new user from Firebase Auth: {}", email);
            LocalDateTime now = LocalDateTime.now();
            user = User.builder()
                    .email(email)
                    .fullName(extractNameFromEmail(email))
                    .role(Role.CUSTOMER)
                    .status(UserStatus.ACTIVE)
                    .firebaseUid(firebaseUid)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            isNewUser = true;
        }
        
        user.setFcmToken(fcmToken);
        User savedUser = userRepository.save(user);
        log.info("FCM token updated successfully for: {}", email);
        
        // Create welcome notification for new users (without FCM push)
        if (isNewUser) {
            try {
                String welcomeTitle = "Chào mừng đến với PandQ! 🎉";
                String welcomeBody = "Chúng tôi rất vui khi bạn đã tham gia. Khám phá ngay những sản phẩm công nghệ hot nhất!";
                notificationService.createNotificationWithoutFcm(
                    savedUser.getId(),
                    NotificationType.SYSTEM,
                    welcomeTitle,
                    welcomeBody,
                    "pandq://home"
                );
                log.info("Welcome notification created for new user: {}", email);
            } catch (Exception e) {
                log.error("Failed to create welcome notification for user {}: {}", email, e.getMessage());
            }
        }
    }

    /**
     * Extract a display name from email address.
     * Example: "john.doe@gmail.com" -> "John Doe"
     */
    private String extractNameFromEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "User";
        }
        String localPart = email.split("@")[0];
        // Replace common separators with spaces
        String name = localPart.replace(".", " ").replace("_", " ").replace("-", " ");
        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1).toLowerCase())
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Close user account permanently.
     * This will:
     * 1. Update user status to CLOSED
     * 2. Disable Firebase account
     * @param email User's email
     * @param reason Optional reason for closing
     */
    @Transactional
    public void closeAccount(String email, String reason) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
        
        if (user.getStatus() == UserStatus.CLOSED) {
            throw new RuntimeException("Account is already closed");
        }
        
        // Update user status to CLOSED
        user.setStatus(UserStatus.CLOSED);
        userRepository.save(user);
        log.info("User account closed: {} - Reason: {}", email, reason != null ? reason : "Not provided");
        
        // Disable Firebase account if firebaseUid exists
        if (user.getFirebaseUid() != null) {
            try {
                com.google.firebase.auth.UserRecord.UpdateRequest updateRequest = 
                    new com.google.firebase.auth.UserRecord.UpdateRequest(user.getFirebaseUid())
                        .setDisabled(true);
                com.google.firebase.auth.FirebaseAuth.getInstance().updateUser(updateRequest);
                log.info("Firebase account disabled for user: {}", email);
            } catch (com.google.firebase.auth.FirebaseAuthException e) {
                log.error("Failed to disable Firebase account for {}: {}", email, e.getMessage());
                // Continue anyway - the local account is already closed
            }
        }
    }
}


