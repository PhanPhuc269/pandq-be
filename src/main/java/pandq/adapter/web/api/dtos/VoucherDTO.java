package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class VoucherDTO {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoucherResponse {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private String discountType;  // PERCENTAGE, FIXED_AMOUNT, FREE_SHIPPING
        private BigDecimal value;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minOrderValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer quantityLimit;
        private Integer usageCount;
        private Boolean isClaimed;     // User đã lưu voucher này chưa
        private Boolean isUsed;        // User đã sử dụng chưa
        private LocalDateTime claimedAt;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimRequest {
        private String promotionId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClaimResponse {
        private Boolean success;
        private String message;
        private VoucherResponse voucher;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoucherListResponse {
        private java.util.List<VoucherResponse> vouchers;
        private Integer totalCount;
    }
}
