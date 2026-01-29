package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.NotificationPreferenceRepository;
import pandq.domain.models.interaction.NotificationPreference;
import pandq.infrastructure.persistence.repositories.jpa.JpaNotificationPreferenceRepository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationPreferenceRepositoryImpl implements NotificationPreferenceRepository {

    private final JpaNotificationPreferenceRepository jpaRepository;

    @Override
    public NotificationPreference save(NotificationPreference preference) {
        return jpaRepository.save(preference);
    }

    @Override
    public Optional<NotificationPreference> findByUserId(UUID userId) {
        return Optional.ofNullable(jpaRepository.findByUserId(userId));
    }
}
