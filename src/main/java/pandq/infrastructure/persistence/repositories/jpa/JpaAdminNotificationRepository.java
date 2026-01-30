package pandq.infrastructure.persistence.repositories.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pandq.domain.models.interaction.AdminNotification;

import java.util.UUID;

@Repository
public interface JpaAdminNotificationRepository extends JpaRepository<AdminNotification, UUID> {
    
    // Find notifications sorted by creation date descending
    Page<AdminNotification> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Count unread notifications
    long countByIsReadFalse();
}
