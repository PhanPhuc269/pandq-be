package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import pandq.domain.models.interaction.Notification;

import java.util.List;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
