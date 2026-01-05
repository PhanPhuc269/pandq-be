package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pandq.domain.models.enums.NotificationType;
import pandq.infrastructure.services.FcmService;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service for sending notifications to Admin app.
 * Uses FCM topic "admin_notifications" for broadcast to all admins.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminNotificationService {

    private final FcmService fcmService;
    private final pandq.infrastructure.persistence.repositories.jpa.JpaAdminNotificationRepository notificationRepository;

    private static final String ADMIN_TOPIC = "admin_notifications";

    /**
     * Notify admins about a new order.
     */
    @Async
    public void notifyNewOrder(UUID orderId, String customerName, BigDecimal totalAmount) {
        String title = "🛒 Đơn hàng mới";
        String body = String.format("Khách: %s - Tổng: %,.0f₫", 
                customerName != null ? customerName : "Khách vãng lai", 
                totalAmount);
        String targetData = "/orders/" + orderId;

        saveAndSend(title, body, NotificationType.NEW_ORDER, targetData);
    }

    /**
     * Notify admins about low stock.
     */
    @Async
    public void notifyLowStock(String productName, int currentStock, int thresholdStock) {
        String title = "⚠️ Sắp hết hàng";
        String body = String.format("%s còn %d sản phẩm (ngưỡng: %d)", 
                productName, currentStock, thresholdStock);
        String targetData = "/inventory";

        saveAndSend(title, body, NotificationType.LOW_STOCK, targetData);
    }

    /**
     * Notify admins about payment received.
     */
    @Async
    public void notifyPaymentReceived(UUID orderId, BigDecimal amount, String paymentMethod) {
        String title = "💰 Thanh toán thành công";
        String body = String.format("Đơn #%s - %,.0f₫ (%s)", 
                orderId.toString().substring(0, 8), amount, paymentMethod);
        String targetData = "/orders/" + orderId;

        saveAndSend(title, body, NotificationType.ADMIN_ALERT, targetData);
    }

    /**
     * Send a general admin alert.
     */
    @Async
    public void sendAdminAlert(String title, String message) {
        saveAndSend("🔔 " + title, message, NotificationType.ADMIN_ALERT, null);
    }

    private void saveAndSend(String title, String body, NotificationType type, String targetData) {
        // 1. Save to DB
        try {
            pandq.domain.models.interaction.AdminNotification notification = pandq.domain.models.interaction.AdminNotification.builder()
                    .title(title)
                    .body(body)
                    .type(type)
                    .targetData(targetData)
                    .isRead(false)
                    .build();
            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("[AdminNotification] Failed to save notification to DB", e);
        }

        // 2. Send FCM
        log.info("[AdminNotification] Sending {} to admins: {}", type, title);
        fcmService.sendToTopicWithData(
                ADMIN_TOPIC,
                title,
                body,
                type,
                targetData
        );
    }
}
