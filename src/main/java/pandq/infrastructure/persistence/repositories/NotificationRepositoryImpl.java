package pandq.infrastructure.persistence.repositories;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pandq.application.port.repositories.NotificationRepository;
import pandq.domain.models.interaction.Notification;
import pandq.infrastructure.persistence.repositories.jpa.JpaNotificationRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JpaNotificationRepository jpaNotificationRepository;

    @Override
    public Notification save(Notification notification) {
        return jpaNotificationRepository.save(notification);
    }

    @Override
    public Optional<Notification> findById(UUID id) {
        return jpaNotificationRepository.findById(id);
    }

    @Override
    public List<Notification> findByUserId(UUID userId) {
        return jpaNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaNotificationRepository.deleteById(id);
    }
}
