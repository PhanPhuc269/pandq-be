package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.NotificationDTO;
import pandq.application.port.repositories.NotificationRepository;
import pandq.application.port.repositories.UserRepository;
import pandq.domain.models.interaction.Notification;
import pandq.domain.models.enums.NotificationType;
import pandq.domain.models.user.User;
import pandq.infrastructure.services.FcmService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;

    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getNotificationsByUserId(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getNotificationsByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return notificationRepository.findByUserId(user.getId()).stream()
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

    /**
     * Create a new notification and send push notification via FCM.
     *
     * @param userId    The user to notify
     * @param type      Notification type
     * @param title     Notification title
     * @param body      Notification body
     * @param targetUrl Optional URL for deep linking
     * @return Created notification response
     */
    @Transactional
    public NotificationDTO.Response createNotification(UUID userId, NotificationType type, 
                                                        String title, String body, String targetUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save notification to database
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .targetUrl(targetUrl)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", userId, title);

        // Send push notification via FCM
        String fcmToken = user.getFcmToken();
        if (fcmToken != null && !fcmToken.isEmpty()) {
            fcmService.sendNotification(fcmToken, title, body, type, targetUrl);
        } else {
            log.warn("User {} does not have FCM token, skipping push notification", userId);
        }

        return mapToResponse(savedNotification);
    }

    /**
     * Create a new notification WITHOUT sending push notification via FCM.
     * Use this for in-app notifications that don't require push (e.g., welcome messages).
     *
     * @param userId    The user to notify
     * @param type      Notification type
     * @param title     Notification title
     * @param body      Notification body
     * @param targetUrl Optional URL for deep linking
     * @return Created notification response
     */
    @Transactional
    public NotificationDTO.Response createNotificationWithoutFcm(UUID userId, NotificationType type, 
                                                        String title, String body, String targetUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save notification to database only, no FCM push
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .body(body)
                .targetUrl(targetUrl)
                .isRead(false)
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Created notification (no FCM) for user {}: {}", userId, title);

        return mapToResponse(savedNotification);
    }

    /**
     * Send notification to all subscribed users (broadcast).
     * Useful for promotions or system announcements.
     *
     * @param topic Topic to send to
     * @param title Notification title
     * @param body  Notification body
     */
    public void sendBroadcastNotification(String topic, String title, String body) {
        fcmService.sendToTopic(topic, title, body);
        log.info("Sent broadcast notification to topic '{}': {}", topic, title);
    }

    /**
     * Create a notification asynchronously (non-blocking).
     * Use this from payment callbacks to avoid blocking the response.
     *
     * @param userId    The user to notify
     * @param type      Notification type
     * @param title     Notification title
     * @param body      Notification body
     * @param targetUrl Optional URL for deep linking
     */
    @Async
    @Transactional
    public void createNotificationAsync(UUID userId, NotificationType type, 
                                        String title, String body, String targetUrl) {
        try {
            createNotification(userId, type, title, body, targetUrl);
            log.info("Async notification created successfully for user {}", userId);
        } catch (Exception e) {
            log.error("Failed to create async notification for user {}: {}", userId, e.getMessage());
        }
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

