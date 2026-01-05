package pandq.application.port.repositories;

import pandq.domain.models.interaction.NotificationTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository {
    NotificationTemplate save(NotificationTemplate template);
    Optional<NotificationTemplate> findById(UUID id);
    List<NotificationTemplate> findAll();
    List<NotificationTemplate> findByIsActive(Boolean isActive);
    List<NotificationTemplate> findScheduledToSend(LocalDateTime before);
    void deleteById(UUID id);
}
