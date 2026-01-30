package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pandq.domain.enums.AccountStatus;
import pandq.domain.enums.CustomerTier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTOs for Customer Management endpoints.
 * Following Clean Architecture - DTOs belong in Adapter layer.
 */
public class CustomerDTO {

    /**
     * Customer list item for table/grid display
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerListItemDto {
        private String id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private CustomerTier customerTier;
        private BigDecimal totalSpent;
        private AccountStatus accountStatus;
        private LocalDateTime createdAt;
        private Long orderCount;
    }

    /**
     * Detailed customer information with order history
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDetailDto {
        private String id;
        private String fullName;
        private String email;
        private String phone;
        private String avatarUrl;
        private CustomerTier customerTier;
        private BigDecimal totalSpent;
        private AccountStatus accountStatus;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private Long orderCount;
        private List<OrderSummaryDto> recentOrders;
    }

    /**
     * Order summary for customer detail
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryDto {
        private String orderId;
        private LocalDateTime orderDate;
        private BigDecimal totalAmount;
        private String status;
        private Integer itemCount;
    }

    /**
     * Customer statistics and tier distribution
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerStatsDto {
        private Long totalCustomers;
        private Long activeCustomers;
        private Long inactiveCustomers;
        private Long bannedCustomers;
        private Map<CustomerTier, Long> tierDistribution;
        private BigDecimal totalRevenue;
    }

    /**
     * Paginated customer list response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerListResponse {
        private List<CustomerListItemDto> customers;
        private Long totalElements;
        private Integer totalPages;
        private Integer currentPage;
        private Integer pageSize;
    }

    /**
     * Request to update customer status
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private AccountStatus status;
    }
}
