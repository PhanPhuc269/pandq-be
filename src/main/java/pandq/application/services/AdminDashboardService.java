package pandq.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pandq.adapter.web.api.dtos.AdminDashboardDTO;
import pandq.domain.models.enums.OrderStatus;
import pandq.domain.models.order.Order;
import pandq.infrastructure.persistence.repositories.jpa.JpaOrderRepository;
import pandq.infrastructure.persistence.repositories.jpa.JpaProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final JpaOrderRepository orderRepository;
    private final JpaProductRepository productRepository;

    public AdminDashboardDTO.SummaryResponse getDashboardSummary() {
        List<Order> allOrders = orderRepository.findAll();
        
        // Calculate revenue from completed orders
        BigDecimal totalRevenue = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalOrders = allOrders.size();
        long pendingOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.CONFIRMED)
                .count();
        long completedOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .count();

        // Count total products as "inventory" indicator (no stock field available)
        long lowStockAlerts = 0; // Simplified - no stock tracking in Product entity

        // Get recent orders as activities
        List<AdminDashboardDTO.RecentActivityResponse> recentActivities = allOrders.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(this::mapToRecentActivity)
                .collect(Collectors.toList());

        return AdminDashboardDTO.SummaryResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueTrend("+12.5%") // Placeholder - could calculate from historical data
                .revenueSubtitle("vs last month")
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .completedOrders(completedOrders)
                .lowStockAlerts(lowStockAlerts)
                .recentActivities(recentActivities)
                .build();
    }

    private AdminDashboardDTO.RecentActivityResponse mapToRecentActivity(Order order) {
        String statusColor = switch (order.getStatus()) {
            case PENDING -> "orange";
            case CONFIRMED -> "blue";
            case SHIPPING -> "purple";
            case DELIVERED, COMPLETED -> "green";
            case CANCELLED, FAILED, RETURNED -> "red";
        };

        String firstProductImage = order.getOrderItems().isEmpty() ? null :
                order.getOrderItems().get(0).getProduct().getThumbnailUrl();

        return AdminDashboardDTO.RecentActivityResponse.builder()
                .id(order.getId().toString())
                .title("Order #" + order.getId().toString().substring(0, 8).toUpperCase())
                .subtitle(order.getTotalAmount() + " VND - " + order.getOrderItems().size() + " items")
                .status(order.getStatus().name())
                .statusColor(statusColor)
                .time(formatTimeAgo(order.getCreatedAt()))
                .imageUrl(firstProductImage)
                .isAlert(order.getStatus() == OrderStatus.PENDING)
                .build();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        Duration duration = Duration.between(dateTime, LocalDateTime.now());
        if (duration.toMinutes() < 60) {
            return duration.toMinutes() + " mins ago";
        } else if (duration.toHours() < 24) {
            return duration.toHours() + " hours ago";
        } else {
            return duration.toDays() + " days ago";
        }
    }
}

