package pandq.adapter.web.api.dtos;

import lombok.Data;

/**
 * DTOs for SePay payment integration using VietQR
 */
public class SepayDTO {

    @Data
    public static class CreateQRRequest {
        private Long amount;
        private String description;
        private String orderId;
    }

    @Data
    public static class CreateQRResponse {
        private Integer returnCode; // 1 = success, 0 = error
        private String returnMessage;
        private String qrDataUrl; // Base64 QR image or URL
        private String qrCode; // Raw QR code content
        private String transactionId;
        private String bankAccount;
        private String bankCode;
        private String accountName;
        private Long amount;
        private String content; // Payment content/reference
    }

    @Data
    public static class WebhookRequest {
        private Long id;
        private String gateway;
        private String transactionDate;
        private String accountNumber;
        private String code; // Payment code extracted from content
        private String content;
        private String transferType; // "in" or "out"
        private Long transferAmount;
        private Long accumulated;
        private String subAccount;
        private String referenceCode;
        private String description;
    }

    @Data
    public static class WebhookResponse {
        private Boolean success;  // SePay requires {"success": true}
        private String message;
    }

    @Data 
    public static class TransactionQueryResponse {
        private Integer returnCode;
        private String returnMessage;
        private Boolean isPaid;
        private Long amount;
        private String transactionDate;
    }
}
