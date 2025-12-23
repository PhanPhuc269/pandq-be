package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.user.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Service for authentication-related operations.
 * Provides helper methods to get current authenticated user from SecurityContext.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;

    /**
     * Get the Firebase UID of the currently authenticated user.
     * @return Firebase UID or null if not authenticated
     */
    public String getCurrentFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null) {
            return auth.getPrincipal().toString();
        }
        return null;
    }

    /**
     * Get the currently authenticated user from database.
     * @return User entity or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<User> getCurrentUser() {
        String firebaseUid = getCurrentFirebaseUid();
        if (firebaseUid == null) {
            log.warn("No authenticated user found in SecurityContext");
            return Optional.empty();
        }
        return userRepository.findByFirebaseUid(firebaseUid);
    }

    /**
     * Get the currently authenticated user's ID.
     * @return User UUID or null if not found
     */
    @Transactional(readOnly = true)
    public UUID getCurrentUserId() {
        return getCurrentUser().map(User::getId).orElse(null);
    }

    /**
     * Get the currently authenticated user's email.
     * @return Email or null if not found
     */
    @Transactional(readOnly = true)
    public String getCurrentUserEmail() {
        return getCurrentUser().map(User::getEmail).orElse(null);
    }
}
