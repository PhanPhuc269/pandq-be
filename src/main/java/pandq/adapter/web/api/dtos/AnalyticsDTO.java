package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTOs for Analytics/Sales Analysis endpoints.
 * Following Clean Architecture - DTOs belong in Adapter layer.
 */
public class AnalyticsDTO {

    /**
     * Response for sales overview KPIs
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesOverviewResponse {
        private BigDecimal totalRevenue;
        private Double revenueChangePercent;
        private Double conversionRate;
        private Double conversionChangePercent;
        private BigDecimal averageOrderValue;
        private Double averageOrderChangePercent;
        private Long totalOrders;
        private Double ordersChangePercent;
        private Long totalProductsSold; // Total quantity of all products sold
        private Double productsChangePercent;
        private String dateRange;
        private String message;
        private Long newCustomers;
        private Double newCustomersChangePercent;
    }

    /**
     * Response for revenue chart data (daily revenue)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RevenueChartResponse {
        private BigDecimal totalRevenue;
        private Double changePercent;
        private String dateRangeLabel;
        private List<DailyRevenue> dailyRevenues;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRevenue {
        private LocalDate date;
        private String dayLabel;
        private BigDecimal revenue;
        private Double percentage; // Relative to max revenue in range (for chart height)
        private Boolean isHighlighted;
    }

    /**
     * Response for top selling products
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProductsResponse {
        private List<TopProduct> products;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private Integer rank;
        private String productId;
        private String productName;
        private String imageUrl;
        private BigDecimal price;
        private Long quantitySold;
        private BigDecimal totalRevenue;
    }

    /**
     * Response for category sales distribution
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySalesResponse {
        private BigDecimal totalRevenue;
        private List<CategorySales> categories;
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySales {
        private String categoryId;
        private String categoryName;
        private BigDecimal revenue;
        private Double percentage;
        private String colorHex; // For chart display
        private Long quantitySold;
        private String imageUrl;
    }

    /**
     * Combined response for full sales analysis page
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FullAnalyticsResponse {
        private SalesOverviewResponse overview;
        private RevenueChartResponse revenueChart;
        private TopProductsResponse topProducts;
        private CategorySalesResponse categorySales;
        private String message;
    }

    /**
     * Response for daily analytics detail (specific date drill-down)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyAnalyticsDetailResponse {
        private LocalDate date;
        private BigDecimal totalRevenue;
        private Long orderCount;
        private List<CategoryRevenueDetail> categories;
        private String message;
    }

    /**
     * Category revenue detail for daily analytics
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryRevenueDetail {
        private String categoryId;
        private String categoryName;
        private BigDecimal revenue;
        private Double percentage;
        private String colorHex;
        private Long quantitySold;
        private String imageUrl;
        private List<ProductRevenueDetail> products;
    }

    /**
     * Product revenue detail within a category
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductRevenueDetail {
        private String productId;
        private String productName;
        private BigDecimal revenue;
        private Long quantitySold;
        private BigDecimal price;
        private String imageUrl;
    }
}
