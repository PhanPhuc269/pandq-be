package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pandq.domain.models.interaction.NotificationPreference;

import java.util.UUID;

@Repository
public interface JpaNotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    NotificationPreference findByUserId(UUID userId);
}
