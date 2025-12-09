package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.NotificationDTO;
import pandq.application.port.repositories.NotificationRepository;
import pandq.domain.models.interaction.Notification;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private NotificationDTO.Response mapToResponse(Notification notification) {
        NotificationDTO.Response response = new NotificationDTO.Response();
        response.setId(notification.getId());
        response.setUserId(notification.getUser().getId());
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setBody(notification.getBody());
        response.setTargetUrl(notification.getTargetUrl());
        response.setIsRead(notification.getIsRead());
        response.setCreatedAt(notification.getCreatedAt());
        return response;
    }
}
