package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ShippingDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalculateRequest {
        private String shippingAddress; // JSON address or formatted string
        private String city;
        private String district;
        private List<CartItem> items;
        private BigDecimal totalAmount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CartItem {
        private UUID productId;
        private Integer quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CalculateResponse {
        private BigDecimal shippingFee;
        private String zoneName;
        private Integer zoneLevel;
        private Integer chargeableWeight; // in grams
        private Boolean isFreeShip;
        private String freeShipReason;
        private BigDecimal freeShipThreshold; // Amount needed for free ship
        private BigDecimal amountToFreeShip;  // How much more to get free ship
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ZoneResponse {
        private UUID id;
        private String name;
        private Integer zoneLevel;
        private BigDecimal baseFee;
        private BigDecimal freeShipThreshold;
    }
}
