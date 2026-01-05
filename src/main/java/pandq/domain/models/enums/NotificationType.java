package pandq.domain.models.enums;

/**
 * Notification types for categorizing and routing notifications.
 * 
 * Customer-facing types:
 * - ORDER_UPDATE: Order status changes (confirmed, shipping, delivered)
 * - PROMOTION: Sales, discounts, vouchers
 * - PAYMENT_SUCCESS: Payment confirmation
 * - SYSTEM: General system announcements
 * 
 * Admin-facing types:
 * - ADMIN_ALERT: General admin alerts
 * - NEW_ORDER: New order received
 * - LOW_STOCK: Inventory warning
 */
public enum NotificationType {
    // Customer notifications
    ORDER_UPDATE,
    PROMOTION,
    PAYMENT_SUCCESS,
    SYSTEM,
    
    // Admin notifications
    ADMIN_ALERT,
    NEW_ORDER,
    LOW_STOCK
}
