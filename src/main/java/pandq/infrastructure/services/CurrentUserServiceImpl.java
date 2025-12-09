package pandq.infrastructure.services;

import java.util.UUID;

import pandq.application.port.services.CurrentUserService;
import pandq.domain.models.user.User;
import pandq.infrastructure.errors.exceptions.UserNotFoundException;
import pandq.infrastructure.persistence.repositories.jpa.JpaUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class CurrentUserServiceImpl implements CurrentUserService {

    private final JpaUserRepository userJpaRepository;

    @Override
    public User getCurrentUser() {
        String firebaseUid = SecurityContextHolder.getContext().getAuthentication().getName();
        return userJpaRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new UserNotFoundException("User not found with UID: " + firebaseUid));
    }

    @Override
    public UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
