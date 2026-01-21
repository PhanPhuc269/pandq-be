package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.enums.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class OrderDTO {

    @Data
    public static class CreateRequest {
        private String userId; // Optional if from token - can be Firebase UID
        private List<OrderItemRequest> items;
        private String shippingAddress;
        private PaymentMethod paymentMethod;
        private String note;
        private UUID promotionId;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public List<OrderItemRequest> getItems() { return items; }
        public void setItems(List<OrderItemRequest> items) { this.items = items; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
        public UUID getPromotionId() { return promotionId; }
        public void setPromotionId(UUID promotionId) { this.promotionId = promotionId; }
    }

    @Data
    public static class OrderItemRequest {
        private UUID productId;
        private Integer quantity;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    @Data
    public static class Response {
        private UUID id;
        private String userId;
        private BigDecimal totalAmount;
        private BigDecimal shippingFee;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private PaymentMethod paymentMethod;
        private OrderStatus status;
        private String shippingAddress;
        private String shippingProvider;
        private String trackingNumber;
        private LocalDateTime createdAt;
        private List<OrderItemResponse> items;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public BigDecimal getShippingFee() { return shippingFee; }
        public void setShippingFee(BigDecimal shippingFee) { this.shippingFee = shippingFee; }
        public BigDecimal getDiscountAmount() { return discountAmount; }
        public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
        public BigDecimal getFinalAmount() { return finalAmount; }
        public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
        public PaymentMethod getPaymentMethod() { return paymentMethod; }
        public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
        public String getShippingAddress() { return shippingAddress; }
        public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public List<OrderItemResponse> getItems() { return items; }
        public void setItems(List<OrderItemResponse> items) { this.items = items; }
    }
    
    @Data
    public static class OrderItemResponse {
        private UUID productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private String imageUrl;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    }

    @Data
    public static class AddToCartRequest {
        private String userId; // Firebase UID or UUID string
        private String productId; // String format for flexibility
        private Integer quantity;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    // ==================== Shipping Management DTOs ====================

    @Data
    public static class AssignCarrierRequest {
        private String shippingProvider; // e.g. "Giao Hàng Nhanh", "Viettel Post"
        private String trackingNumber;   // Optional tracking number
    }

    @Data
    public static class UpdateStatusRequest {
        private OrderStatus status;

        public OrderStatus getStatus() { return status; }
        public void setStatus(OrderStatus status) { this.status = status; }
    }

    @Data
    public static class ApplyPromotionRequest {
        private String userId; // For voucher validation
        private UUID promotionId; // The voucher/promotion to apply
    }        
}
