package pandq.adapter.web.api.dtos;

import lombok.Data;
import pandq.domain.models.enums.DiscountType;
import pandq.domain.models.enums.Status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class PromotionDTO {

    @Data
    public static class CreateRequest {
        private String code;
        private String name;
        private String description;
        private DiscountType type;
        private BigDecimal value;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minOrderValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer quantityLimit;
        private List<UUID> applicableCategoryIds;
        private List<UUID> applicableProductIds;
    }

    @Data
    public static class UpdateRequest {
        private String name;
        private BigDecimal value;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minOrderValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer quantityLimit;
        private Status status;
        private List<UUID> applicableCategoryIds;
        private List<UUID> applicableProductIds;
    }

    @Data
    public static class Response {
        private UUID id;
        private String code;
        private String name;
        private String description;
        private DiscountType type;
        private BigDecimal value;
        private BigDecimal maxDiscountAmount;
        private BigDecimal minOrderValue;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer quantityLimit;
        private Integer usageCount;
        private Status status;
        private List<UUID> applicableCategoryIds;
        private List<UUID> applicableProductIds;
    }

    // DTO để validate mã giảm giá
    @Data
    public static class ValidateRequest {
        private String promoCode;
        private BigDecimal orderTotal;
        private List<UUID> productIds;
        private List<UUID> categoryIds;
    }

    // DTO trả về kết quả validation
    @Data
    public static class ValidateResponse {
        private boolean valid;
        private String message;
        private BigDecimal discountAmount;
        private BigDecimal finalAmount;
        private Response promotion;

        public static ValidateResponse success(BigDecimal discountAmount, BigDecimal finalAmount, Response promotion) {
            ValidateResponse response = new ValidateResponse();
            response.setValid(true);
            response.setMessage("Áp dụng mã giảm giá thành công!");
            response.setDiscountAmount(discountAmount);
            response.setFinalAmount(finalAmount);
            response.setPromotion(promotion);
            return response;
        }

        public static ValidateResponse error(String message) {
            ValidateResponse response = new ValidateResponse();
            response.setValid(false);
            response.setMessage(message);
            response.setDiscountAmount(BigDecimal.ZERO);
            return response;
        }
    }
}
