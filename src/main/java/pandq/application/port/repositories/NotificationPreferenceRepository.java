package pandq.application.port.repositories;

import pandq.domain.models.interaction.NotificationPreference;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository {
    NotificationPreference save(NotificationPreference preference);
    Optional<NotificationPreference> findByUserId(UUID userId);
}
