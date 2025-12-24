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
    }

    @Data
    public static class OrderItemRequest {
        private UUID productId;
        private Integer quantity;
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
        private LocalDateTime createdAt;
        private List<OrderItemResponse> items;
    }
    
    @Data
    public static class OrderItemResponse {
        private UUID productId;
        private String productName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal totalPrice;
        private String imageUrl;
    }

    @Data
    public static class AddToCartRequest {
        private String userId; // Firebase UID or UUID string
        private UUID productId;
        private Integer quantity;
    }
}
