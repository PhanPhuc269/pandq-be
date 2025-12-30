package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pandq.domain.models.interaction.NotificationTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface JpaNotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    List<NotificationTemplate> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);
    List<NotificationTemplate> findAllByOrderByCreatedAtDesc();
    
    @Query("SELECT t FROM NotificationTemplate t WHERE t.isActive = true AND t.scheduledAt IS NOT NULL AND t.scheduledAt <= :before")
    List<NotificationTemplate> findScheduledToSend(@Param("before") LocalDateTime before);
}
