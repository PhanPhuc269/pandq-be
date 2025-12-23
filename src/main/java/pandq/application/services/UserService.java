package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.UserDTO;
import pandq.application.port.repositories.UserRepository;
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
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .avatarUrl(request.getAvatarUrl())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
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
        }
        
        user.setFcmToken(fcmToken);
        userRepository.save(user);
        log.info("FCM token updated successfully for: {}", email);
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
}


