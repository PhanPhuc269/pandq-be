package pandq.infrastructure.services;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pandq.domain.models.enums.NotificationType;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {

    /**
     * Send a push notification to a specific device.
     * Uses DATA-ONLY message so the app has full control over notification display and click handling.
     * This ensures deep links work correctly when user clicks the notification.
     *
     * @param fcmToken  The FCM token of the target device
     * @param title     Notification title
     * @param body      Notification body
     * @param type      Notification type for client-side handling
     * @param targetUrl Optional URL for deep linking
     * @return Message ID if successful, null otherwise
     */
    public String sendNotification(String fcmToken, String title, String body, 
                                   NotificationType type, String targetUrl) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Cannot send notification: FCM token is null or empty");
            return null;
        }

        try {
            // Build DATA-ONLY message (no notification payload)
            // This ensures the app receives data even when in background/killed
            // and can properly handle deep links on click
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .putData("title", title)
                    .putData("body", body)
                    .putData("type", type != null ? type.name() : "SYSTEM")
                    // High priority to wake up device and deliver message reliably
                    .setAndroidConfig(com.google.firebase.messaging.AndroidConfig.builder()
                            .setPriority(com.google.firebase.messaging.AndroidConfig.Priority.HIGH)
                            .build());

            if (targetUrl != null && !targetUrl.isEmpty()) {
                messageBuilder.putData("targetUrl", targetUrl);
            }

            Message message = messageBuilder.build();

            // Send the message
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM data message. Message ID: {}, targetUrl: {}", messageId, targetUrl);
            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to token: {}. Error: {}", 
                    fcmToken.substring(0, Math.min(10, fcmToken.length())) + "...", 
                    e.getMessage());
            return null;
        }
    }

    /**
     * Send a push notification with custom data payload.
     *
     * @param fcmToken The FCM token of the target device
     * @param title    Notification title
     * @param body     Notification body
     * @param data     Additional data to include in the message
     * @return Message ID if successful, null otherwise
     */
    public String sendNotificationWithData(String fcmToken, String title, String body, 
                                           Map<String, String> data) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            log.warn("Cannot send notification: FCM token is null or empty");
            return null;
        }

        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (data != null) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM notification with data. Message ID: {}", messageId);
            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Send a notification to a topic (for broadcast notifications like promotions).
     *
     * @param topic Topic name (e.g., "promotions", "order_updates")
     * @param title Notification title
     * @param body  Notification body
     * @return Message ID if successful, null otherwise
     */
    public String sendToTopic(String topic, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .build();

            String messageId = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM notification to topic '{}'. Message ID: {}", topic, messageId);
            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM notification to topic '{}': {}", topic, e.getMessage());
            return null;
        }
    }
}
