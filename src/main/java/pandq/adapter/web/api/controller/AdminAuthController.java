package pandq.adapter.web.api.controller;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.AdminAuthDTO;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.enums.Role;
import pandq.domain.models.user.User;

import java.util.Optional;

/**
 * Controller for admin authentication endpoints.
 * Verifies if the authenticated user has admin privileges.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AdminAuthController {

    private final UserRepository userRepository;
    private final pandq.application.services.UserService userService;

    /**
     * Verifies if the currently authenticated user is an admin.
     * The user must be authenticated via Firebase token in the Authorization header.
     * 
     * This method will:
     * 1. Get user by firebaseUid
     * 2. If not found, get user by email and link the firebaseUid
     *
     * @return VerifyAdminResponse with isAdmin flag and user details
     */
    @GetMapping("/verify-admin")
    public ResponseEntity<AdminAuthDTO.VerifyAdminResponse> verifyAdmin(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            // Get Firebase token
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String firebaseUid = decodedToken.getUid();
            String email = decodedToken.getEmail();

            log.info("Verifying admin for email: {}, uid: {}", email, firebaseUid);

            // First try to find by firebaseUid
            Optional<User> userOpt = userRepository.findByFirebaseUid(firebaseUid);

            // If not found by UID, try to find by email (for seeded users)
            if (userOpt.isEmpty() && email != null) {
                userOpt = userRepository.findByEmail(email);
                
                // If found by email, link the firebaseUid
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setFirebaseUid(firebaseUid);
                    userRepository.save(user);
                    log.info("Linked firebaseUid {} to user {}", firebaseUid, email);
                }
            }

            if (userOpt.isEmpty()) {
                log.warn("Admin verification failed: User not found for email {} or uid {}", email, firebaseUid);
                return ResponseEntity.ok(AdminAuthDTO.VerifyAdminResponse.builder()
                        .isAdmin(false)
                        .message("User not found. Please register first.")
                        .build());
            }

            User user = userOpt.get();
            boolean isAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;

            if (!isAdmin) {
                log.warn("Admin verification failed: User {} is not an admin (role: {})",
                        user.getEmail(), user.getRole());
                return ResponseEntity.ok(AdminAuthDTO.VerifyAdminResponse.builder()
                        .isAdmin(false)
                        .message("You don't have admin privileges.")
                        .user(mapToUserInfo(user))
                        .build());
            }

            log.info("Admin verification successful: {} (role: {})", user.getEmail(), user.getRole());
            return ResponseEntity.ok(AdminAuthDTO.VerifyAdminResponse.builder()
                    .isAdmin(true)
                    .message("Admin verified successfully.")
                    .user(mapToUserInfo(user))
                    .build());

        } catch (Exception e) {
            log.error("Error verifying admin", e);
            return ResponseEntity.ok(AdminAuthDTO.VerifyAdminResponse.builder()
                    .isAdmin(false)
                    .message("Authentication error: " + e.getMessage())
                    .build());
        }
    }

    /**
     * Get current user info for the authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<AdminAuthDTO.UserInfo> getCurrentUser(
            @RequestHeader("Authorization") String authHeader
    ) {
        try {
            String idToken = authHeader.replace("Bearer ", "");
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            String firebaseUid = decodedToken.getUid();

            Optional<User> userOpt = userRepository.findByFirebaseUid(firebaseUid);

            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(mapToUserInfo(userOpt.get()));
        } catch (Exception e) {
            log.error("Error getting current user", e);
            return ResponseEntity.badRequest().build();
        }
    }

    private AdminAuthDTO.UserInfo mapToUserInfo(User user) {
        return AdminAuthDTO.UserInfo.builder()
                .id(user.getId().toString())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
    
    @PostMapping("/demote")
    public ResponseEntity<Void> demoteAdmin(@RequestBody DemoAdminRequest request) {
        // In real app, check if current user is SUPER_ADMIN
        userService.demoteToCustomer(request.getEmail());
        return ResponseEntity.ok().build();
    }
    
    @lombok.Data
    public static class DemoAdminRequest {
        private String email;
    }
}
