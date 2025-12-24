package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderPaymentDetailsResponse {
        private String orderId;
        private String orderStatus;
        private String paymentMethod;
        
        // User info
        private String userId;
        private String userName;
        private String userEmail;
        private String userPhone;
        
        // Shipping address
        private String shippingAddress;
        private String shippingCity;
        private String shippingDistrict;
        
        // Order items
        private List<OrderItemDetail> items;
        
        // Amounts
        private Long subtotal;
        private Long shippingFee;
        private Long discountAmount;
        private Long finalAmount;
        
        // Additional info
        private String orderNote;
        private LocalDateTime createdAt;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDetail {
        private String productId;
        private String productName;
        private Long price;
        private Integer quantity;
        private Long totalPrice;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetPaymentMethodsResponse {
        private List<PaymentMethodInfo> methods;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentMethodInfo {
        private String id;
        private String name;
        private String displayName;
        private String description;
        private String icon;
        private Boolean isActive;
        private Boolean isEnabled;
        private String processingTime;
        private String method;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitiatePaymentRequest {
        private String orderId;
        private String paymentMethod;
        private Long amount;
        private String description;
        private String userId;
        private String returnUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InitiatePaymentResponse {
        private String transactionId;
        private String paymentMethod;
        private String status;
        private Long amount;
        private LocalDateTime createdAt;
        private PaymentData paymentData;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentData {
        private String orderId;
        private String appTransId;
        private String zpTransToken;
        private String qrDataUrl;
        private String qrUrl;
        private String qrCode;
        private String bankAccount;
        private String accountName;
        private String bankCode;
        private String content;
        private String paymentContent;
        private String orderUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckPaymentStatusResponse {
        private String transactionId;
        private String status;
        private String paymentMethod;
        private Long amount;
        private Boolean isPaid;
        private Boolean isProcessing;
        private LocalDateTime paidAt;
        private String returnMessage;
        private Integer returnCode;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GetPaymentHistoryResponse {
        private List<PaymentHistoryItem> history;
        private Integer total;
        private Integer totalCount;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentHistoryItem {
        private String transactionId;
        private String orderId;
        private String paymentMethod;
        private Long amount;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelPaymentRequest {
        private String transactionId;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CancelPaymentResponse {
        private String transactionId;
        private String status;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentStatisticsResponse {
        private Long totalAmount;
        private Long totalRevenue;
        private Long totalTransactions;
        private Long successfulTransactions;
        private Long failedTransactions;
        private Double successRate;
        private Long pendingAmount;
        private String mostUsedPaymentMethod;
        private String message;
    }
}

