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
}
