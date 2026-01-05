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

    private static final String ADMIN_TOPIC = "admin_notifications";

    /**
     * Notify admins about a new order.
     *
     * @param orderId      Order ID
     * @param customerName Customer name
     * @param totalAmount  Order total
     */
    @Async
    public void notifyNewOrder(UUID orderId, String customerName, BigDecimal totalAmount) {
        String title = "🛒 Đơn hàng mới";
        String body = String.format("Khách: %s - Tổng: %,.0f₫", 
                customerName != null ? customerName : "Khách vãng lai", 
                totalAmount);
        
        log.info("[AdminNotification] Sending NEW_ORDER to admins: orderId={}", orderId);
        fcmService.sendToTopicWithData(
                ADMIN_TOPIC,
                title,
                body,
                NotificationType.NEW_ORDER,
                "/orders/" + orderId
        );
    }

    /**
     * Notify admins about low stock.
     *
     * @param productName    Product name
     * @param currentStock   Current stock quantity
     * @param thresholdStock Threshold that triggered the alert
     */
    @Async
    public void notifyLowStock(String productName, int currentStock, int thresholdStock) {
        String title = "⚠️ Sắp hết hàng";
        String body = String.format("%s còn %d sản phẩm (ngưỡng: %d)", 
                productName, currentStock, thresholdStock);
        
        log.info("[AdminNotification] Sending LOW_STOCK alert: product={}, stock={}", 
                productName, currentStock);
        fcmService.sendToTopicWithData(
                ADMIN_TOPIC,
                title,
                body,
                NotificationType.LOW_STOCK,
                "/inventory"
        );
    }

    /**
     * Notify admins about payment received.
     *
     * @param orderId     Order ID
     * @param amount      Payment amount
     * @param paymentMethod Payment method (ZaloPay, Sepay, etc.)
     */
    @Async
    public void notifyPaymentReceived(UUID orderId, BigDecimal amount, String paymentMethod) {
        String title = "💰 Thanh toán thành công";
        String body = String.format("Đơn #%s - %,.0f₫ (%s)", 
                orderId.toString().substring(0, 8), amount, paymentMethod);
        
        log.info("[AdminNotification] Sending PAYMENT_RECEIVED to admins: orderId={}", orderId);
        fcmService.sendToTopicWithData(
                ADMIN_TOPIC,
                title,
                body,
                NotificationType.ADMIN_ALERT,
                "/orders/" + orderId
        );
    }

    /**
     * Send a general admin alert.
     *
     * @param title   Alert title
     * @param message Alert message
     */
    @Async
    public void sendAdminAlert(String title, String message) {
        log.info("[AdminNotification] Sending ADMIN_ALERT: {}", title);
        fcmService.sendToTopicWithData(
                ADMIN_TOPIC,
                "🔔 " + title,
                message,
                NotificationType.ADMIN_ALERT,
                null
        );
    }
}
