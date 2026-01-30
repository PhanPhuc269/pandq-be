package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.enums.CustomerTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Customer Tier Configuration endpoints.
 */
public class CustomerTierConfigDTO {

    /**
     * Response DTO for tier configuration
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierConfigDto {
        private String id;
        private CustomerTier tier;
        private BigDecimal minSpent;
        private BigDecimal maxSpent;
        private String displayName;
        private String description;
        private Boolean isActive;
        private LocalDateTime updatedAt;
        private String updatedBy;
    }

    /**
     * Response containing all tier configurations
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TierConfigListResponse {
        private List<TierConfigDto> configs;
    }

    /**
     * Request to update a single tier configuration
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTierConfigRequest {
        private CustomerTier tier;
        private BigDecimal minSpent;
        private BigDecimal maxSpent;
        private String displayName;
        private String description;
    }

    /**
     * Request to update all tier configurations
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateAllTierConfigsRequest {
        private List<UpdateTierConfigRequest> configs;
    }
}
