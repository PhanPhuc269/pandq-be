package pandq.adapter.web.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class AdminDashboardDTO {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private BigDecimal totalRevenue;
        private String revenueTrend; // e.g., "+12.5%"
        private String revenueSubtitle; // e.g., "vs last month"
        
        private Long totalOrders;
        private Long pendingOrders;
        private Long completedOrders;
        
        private Long lowStockAlerts;
        
        private List<RecentActivityResponse> recentActivities;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityResponse {
        private String id;
        private String title;
        private String subtitle;
        private String status;
        private String statusColor; // e.g., "green", "orange", "red"
        private String time; // e.g., "2 mins ago"
        private String imageUrl;
        private boolean isAlert;
    }
}
