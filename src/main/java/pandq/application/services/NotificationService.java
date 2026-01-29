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
    private final pandq.application.port.repositories.NotificationPreferenceRepository preferenceRepository;

    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getNotificationsByUserId(UUID userId, NotificationType type) {
        List<Notification> notifications = notificationRepository.findByUserId(userId);
        
        if (type != null) {
            notifications = notifications.stream()
                    .filter(n -> n.getType() == type)
                    .collect(Collectors.toList());
        }
        
        return notifications.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDTO.Response> getNotificationsByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        return getNotificationsByUserId(user.getId(), null);
    }
    
    @Transactional(readOnly = true)
    public NotificationDTO.PreferenceResponse getPreferences(UUID userId) {
        pandq.domain.models.interaction.NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(pandq.domain.models.interaction.NotificationPreference.builder()
                        .user(userRepository.findById(userId).orElseThrow())
                        .build());
        
        NotificationDTO.PreferenceResponse response = new NotificationDTO.PreferenceResponse();
        response.setEnablePromotions(pref.getEnablePromotions());
        response.setEnableOrders(pref.getEnableOrders());
        response.setEnableSystem(pref.getEnableSystem());
        response.setEnableChat(pref.getEnableChat());
        return response;
    }

    @Transactional
    public void updatePreferences(UUID userId, NotificationDTO.PreferenceRequest request) {
        pandq.domain.models.interaction.NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElse(pandq.domain.models.interaction.NotificationPreference.builder()
                        .user(userRepository.findById(userId).orElseThrow())
                        .build());
        
        if (request.getEnablePromotions() != null) pref.setEnablePromotions(request.getEnablePromotions());
        if (request.getEnableOrders() != null) pref.setEnableOrders(request.getEnableOrders());
        if (request.getEnableSystem() != null) pref.setEnableSystem(request.getEnableSystem());
        if (request.getEnableChat() != null) pref.setEnableChat(request.getEnableChat());
        
        preferenceRepository.save(pref);
    }

    @Transactional
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    private boolean isNotificationEnabled(UUID userId, NotificationType type) {
        // If critical notification, always allow? Maybe not.
        // Let's check preferences.
        return preferenceRepository.findByUserId(userId)
                .map(pref -> switch (type) {
                    case PROMOTION -> pref.getEnablePromotions();
                    case ORDER_UPDATE, PAYMENT_SUCCESS -> pref.getEnableOrders();
                    case SYSTEM -> pref.getEnableSystem();
                    case CHAT_MESSAGE -> pref.getEnableChat();
                    default -> true;
                })
                .orElse(true); // Default to true if no preference set
    }

    /**
     * Create a new notification and send push notification via FCM.
     * Notification is ALWAYS saved to database (appears in notification list).
     * FCM push is only sent if user has enabled this notification type.
     */
    @Transactional
    public NotificationDTO.Response createNotification(UUID userId, NotificationType type, 
                                                        String title, String body, String targetUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Always save notification to database (appears in notification list)
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

        // Only send FCM push if user has enabled this notification type
        if (!isNotificationEnabled(userId, type)) {
            log.info("FCM push skipped for user {} type {} (preference disabled)", userId, type);
            return mapToResponse(savedNotification);
        }

        // Send push notification via FCM
        String fcmToken = user.getFcmToken();
        if (fcmToken != null && !fcmToken.isEmpty()) {
            fcmService.sendNotification(fcmToken, title, body, type, targetUrl);
        } else {
            log.warn("User {} does not have FCM token, skipping push notification", userId);
        }

        return mapToResponse(savedNotification);
    }

    // ... (keep createNotificationWithoutFcm and other methods but add check if needed) ...

    @Transactional
    public NotificationDTO.Response createNotificationWithoutFcm(UUID userId, NotificationType type, 
                                                        String title, String body, String targetUrl) {
        // No preference check here - this method always saves to DB (no FCM involved)
        
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
    
    // ... kept broadcast logic ...
    public void sendBroadcastNotification(String topic, String title, String body) {
        fcmService.sendToTopic(topic, title, body);
        log.info("Sent broadcast notification to topic '{}': {}", topic, title);
    }

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

