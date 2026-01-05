package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.NotificationTemplateRepository;
import pandq.domain.models.interaction.NotificationTemplate;
import pandq.infrastructure.persistence.repositories.jpa.JpaNotificationTemplateRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationTemplateRepositoryImpl implements NotificationTemplateRepository {

    private final JpaNotificationTemplateRepository jpaRepository;

    @Override
    public NotificationTemplate save(NotificationTemplate template) {
        return jpaRepository.save(template);
    }

    @Override
    public Optional<NotificationTemplate> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<NotificationTemplate> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<NotificationTemplate> findByIsActive(Boolean isActive) {
        return jpaRepository.findByIsActiveOrderByCreatedAtDesc(isActive);
    }

    @Override
    public List<NotificationTemplate> findScheduledToSend(LocalDateTime before) {
        return jpaRepository.findScheduledToSend(before);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
