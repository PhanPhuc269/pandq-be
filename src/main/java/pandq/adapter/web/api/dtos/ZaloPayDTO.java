package pandq.adapter.web.api.dtos;

import lombok.Data;

/**
 * DTOs for ZaloPay payment integration
 */
public class ZaloPayDTO {

    @Data
    public static class CreateOrderRequest {
        private Long amount;
        private String description;
        private String userId;
        private String orderId;
    }

    @Data
    public static class CreateOrderResponse {
        private Integer returnCode;
        private String returnMessage;
        private String zpTransToken;
        private String orderUrl;
        private String appTransId;
    }

    @Data
    public static class CallbackRequest {
        private String data;
        private String mac;
        private Integer type;
    }

    @Data
    public static class CallbackResponse {
        private Integer returnCode;
        private String returnMessage;
    }

    @Data
    public static class QueryStatusRequest {
        private String appId;
        private String appTransId;
        private String mac;
    }

    @Data
    public static class QueryStatusResponse {
        private Integer returnCode;
        private String returnMessage;
        private Boolean isProcessing;
        private Long amount;
        private String zpTransId;
    }
}
