package pandq.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pandq.adapter.web.api.dtos.AnalyticsDTO;
import pandq.application.port.repositories.OrderRepository;
import pandq.application.port.repositories.CategoryRepository;
import pandq.domain.models.order.Order;
import pandq.domain.models.enums.OrderStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analytics Service - Business logic for sales analytics.
 * Following Clean Architecture - Service belongs in Application layer.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;

    // Chart colors for category distribution
    private static final String[] CATEGORY_COLORS = {
            "#60a5fa", // blue-400
            "#c084fc", // purple-400
            "#f87171", // red-400
            "#4ade80", // green-400
            "#fbbf24", // yellow-400
            "#f472b6" // pink-400
    };

    /**
     * Get sales overview KPIs for the specified date range
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.SalesOverviewResponse getSalesOverview(String range) {
        log.info("Getting sales overview for range: {}", range);

        DateRange dateRange = parseDateRange(range);
        LocalDateTime startDate = dateRange.start;
        LocalDateTime endDate = dateRange.end;
        LocalDateTime previousPeriodStartDate = startDate.minusDays(dateRange.days);
        LocalDateTime previousPeriodEndDate = startDate.minusSeconds(1);

        // Get orders in current period
        List<Order> currentOrders = getCompletedOrdersInRange(startDate, endDate);
        List<Order> previousOrders = getCompletedOrdersInRange(previousPeriodStartDate, previousPeriodEndDate);

        // Calculate New Customers
        long newCustomers = orderRepository.countNewCustomersInRange(startDate, endDate);
        long previousNewCustomers = orderRepository.countNewCustomersInRange(previousPeriodStartDate,
                previousPeriodEndDate);
        Double newCustomersChange = calculatePercentChange(BigDecimal.valueOf(previousNewCustomers),
                BigDecimal.valueOf(newCustomers));

        // Calculate KPIs
        BigDecimal totalRevenue = calculateTotalRevenue(currentOrders);
        BigDecimal previousRevenue = calculateTotalRevenue(previousOrders);
        Double revenueChange = calculatePercentChange(previousRevenue, totalRevenue);

        long totalOrders = currentOrders.size();
        long previousOrderCount = previousOrders.size();
        Double ordersChange = calculatePercentChange(BigDecimal.valueOf(previousOrderCount),
                BigDecimal.valueOf(totalOrders));

        BigDecimal avgOrderValue = totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal previousAvg = previousOrderCount > 0
                ? previousRevenue.divide(BigDecimal.valueOf(previousOrderCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Double avgChange = calculatePercentChange(previousAvg, avgOrderValue);

        // Conversion rate (mock - would need visitor data)
        Double conversionRate = 3.5;
        Double conversionChange = -1.8;

        // Calculate total products sold (sum of all order item quantities)
        long totalProductsSold = currentOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();

        long previousProductsSold = previousOrders.stream()
                .flatMap(order -> order.getOrderItems().stream())
                .mapToLong(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                .sum();

        Double productsChangePercent = calculatePercentChange(BigDecimal.valueOf(previousProductsSold),
                BigDecimal.valueOf(totalProductsSold));

        return AnalyticsDTO.SalesOverviewResponse.builder()
                .totalRevenue(totalRevenue)
                .revenueChangePercent(revenueChange)
                .conversionRate(conversionRate)
                .conversionChangePercent(conversionChange)
                .averageOrderValue(avgOrderValue)
                .averageOrderChangePercent(avgChange)
                .totalOrders(totalOrders)
                .ordersChangePercent(ordersChange)
                .totalProductsSold(totalProductsSold)
                .productsChangePercent(productsChangePercent)
                .newCustomers(newCustomers)
                .newCustomersChangePercent(newCustomersChange)
                .dateRange(range)
                .message("Sales overview retrieved successfully")
                .build();
    }

    /**
     * Get daily revenue chart data
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.RevenueChartResponse getRevenueChart(String range) {
        log.info("Getting revenue chart for range: {}", range);

        DateRange dateRange = parseDateRange(range);
        LocalDateTime startDate = dateRange.start;
        LocalDateTime endDate = dateRange.end;

        List<Order> orders = getCompletedOrdersInRange(startDate, endDate);

        // Group orders by date
        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();

        // Initialize map with all dates in range with 0 revenue
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate.toLocalDate(), endDate.toLocalDate()) + 1;
        // Limit max days for chart to reasonable amount if custom range is huge?
        // For now assume user is reasonable or UI limits it.

        for (int i = 0; i < days; i++) {
            dailyRevenue.put(startDate.toLocalDate().plusDays(i), BigDecimal.ZERO);
        }

        // Sum revenue by date
        for (Order order : orders) {
            if (order.getCreatedAt() != null) {
                LocalDate orderDate = order.getCreatedAt().toLocalDate();
                BigDecimal amount = order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO;
                dailyRevenue.merge(orderDate, amount, BigDecimal::add);
            }
        }

        // Find max for percentage calculation
        BigDecimal maxRevenue = dailyRevenue.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ONE);

        // Convert to response format
        List<AnalyticsDTO.DailyRevenue> dailyList = new ArrayList<>();
        LocalDate today = LocalDate.now();

        int i = 0;
        for (Map.Entry<LocalDate, BigDecimal> entry : dailyRevenue.entrySet()) {
            LocalDate date = entry.getKey();
            BigDecimal revenue = entry.getValue();

            double percentage = maxRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? revenue.divide(maxRevenue, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            dailyList.add(AnalyticsDTO.DailyRevenue.builder()
                    .date(date)
                    .dayLabel(getDayLabel(date, days, i))
                    .revenue(revenue)
                    .percentage(percentage)
                    .isHighlighted(date.equals(today))
                    .build());
            i++;
        }

        BigDecimal totalRevenue = dailyRevenue.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String dateRangeLabel = formatDateRange(startDate.toLocalDate(), endDate.toLocalDate());

        return AnalyticsDTO.RevenueChartResponse.builder()
                .totalRevenue(totalRevenue)
                .changePercent(15.0) // Mock change percent
                .dateRangeLabel(dateRangeLabel)
                .dailyRevenues(dailyList)
                .message("Revenue chart data retrieved successfully")
                .build();
    }

    /**
     * Get top selling products
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.TopProductsResponse getTopProducts(int limit, String range, String sortBy) {
        log.info("Getting top {} selling products for range: {}, sortBy: {}", limit, range, sortBy);

        DateRange dateRange = parseDateRange(range);
        List<Order> orders = getCompletedOrdersInRange(dateRange.start, dateRange.end);

        // Aggregate sales by product
        Map<String, ProductSalesData> productSales = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (var item : order.getOrderItems()) {
                    if (item.getProduct() != null) {
                        String productId = item.getProduct().getId().toString();
                        ProductSalesData data = productSales.getOrDefault(productId, new ProductSalesData());
                        data.productId = productId;
                        data.productName = item.getProduct().getName();
                        data.imageUrl = getProductImageUrl(item.getProduct());
                        data.price = item.getProduct().getPrice();
                        data.quantitySold += item.getQuantity() != null ? item.getQuantity() : 0;
                        data.totalRevenue = data.totalRevenue.add(
                                item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                        productSales.put(productId, data);
                    }
                }
            }
        }

        // Sort by quantity sold or revenue and get top N
        List<AnalyticsDTO.TopProduct> topProducts = productSales.values().stream()
                .sorted((a, b) -> {
                    if ("revenue".equalsIgnoreCase(sortBy)) {
                        return b.totalRevenue.compareTo(a.totalRevenue);
                    }
                    return Long.compare(b.quantitySold, a.quantitySold);
                })
                .limit(limit)
                .map(data -> AnalyticsDTO.TopProduct.builder()
                        .rank(0) // Will be set below
                        .productId(data.productId)
                        .productName(data.productName)
                        .imageUrl(data.imageUrl)
                        .price(data.price)
                        .quantitySold(data.quantitySold)
                        .totalRevenue(data.totalRevenue)
                        .build())
                .collect(Collectors.toList());

        // Set ranks
        for (int i = 0; i < topProducts.size(); i++) {
            topProducts.get(i).setRank(i + 1);
        }

        return AnalyticsDTO.TopProductsResponse.builder()
                .products(topProducts)
                .message("Top products retrieved successfully")
                .build();
    }

    /**
     * Get category sales distribution
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.CategorySalesResponse getCategorySales(String range, String sortBy) {
        log.info("Getting category sales distribution for range: {}, sortBy: {}", range, sortBy);

        DateRange dateRange = parseDateRange(range);
        List<Order> orders = getCompletedOrdersInRange(dateRange.start, dateRange.end);

        // Aggregate sales by category
        Map<String, CategorySalesData> categorySales = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (var item : order.getOrderItems()) {
                    if (item.getProduct() != null && item.getProduct().getCategory() != null) {
                        var category = item.getProduct().getCategory();
                        String categoryId = category.getId().toString();
                        CategorySalesData data = categorySales.getOrDefault(categoryId, new CategorySalesData());
                        data.categoryId = categoryId;
                        data.categoryName = category.getName();
                        data.imageUrl = category.getImageUrl();
                        data.revenue = data.revenue.add(
                                item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO);
                        data.quantitySold += item.getQuantity() != null ? item.getQuantity() : 0;
                        categorySales.put(categoryId, data);
                    }
                }
            }
        }

        // Calculate total and percentages
        BigDecimal totalRevenue = categorySales.values().stream()
                .map(d -> d.revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnalyticsDTO.CategorySales> categoryList = new ArrayList<>();
        int colorIndex = 0;

        for (CategorySalesData data : categorySales.values()) {
            double percentage = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? data.revenue.divide(totalRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            categoryList.add(AnalyticsDTO.CategorySales.builder()
                    .categoryId(data.categoryId)
                    .categoryName(data.categoryName)
                    .revenue(data.revenue)
                    .percentage(percentage)
                    .colorHex(CATEGORY_COLORS[colorIndex % CATEGORY_COLORS.length])
                    .quantitySold(data.quantitySold)
                    .imageUrl(data.imageUrl)
                    .build());
            colorIndex++;
        }

        // Sort by quantity or revenue (percentage)
        categoryList.sort((a, b) -> {
            if ("quantity".equalsIgnoreCase(sortBy)) {
                return Long.compare(b.getQuantitySold(), a.getQuantitySold());
            }
            return Double.compare(b.getPercentage(), a.getPercentage());
        });

        return AnalyticsDTO.CategorySalesResponse.builder()
                .totalRevenue(totalRevenue)
                .categories(categoryList)
                .message("Category sales retrieved successfully")
                .build();
    }

    /**
     * Get full analytics data in one call
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.FullAnalyticsResponse getFullAnalytics(String range) {
        return AnalyticsDTO.FullAnalyticsResponse.builder()
                .overview(getSalesOverview(range))
                .revenueChart(getRevenueChart(range))
                .topProducts(getTopProducts(4, range, "revenue"))
                .categorySales(getCategorySales(range, "revenue"))
                .message("Full analytics data retrieved successfully")
                .build();
    }

    /**
     * Get detailed analytics for a specific date
     */
    @Transactional(readOnly = true)
    public AnalyticsDTO.DailyAnalyticsDetailResponse getDailyAnalyticsDetail(LocalDate date) {
        log.info("Getting daily analytics detail for date: {}", date);

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        // Get orders for this specific date
        List<Order> orders = getCompletedOrdersInRange(startOfDay, endOfDay);

        // Calculate total revenue and order count
        BigDecimal totalRevenue = calculateTotalRevenue(orders);
        long orderCount = orders.size();

        // Aggregate by category and products
        Map<String, CategoryData> categoryDataMap = new HashMap<>();

        for (Order order : orders) {
            if (order.getOrderItems() != null) {
                for (var item : order.getOrderItems()) {
                    if (item.getProduct() != null && item.getProduct().getCategory() != null) {
                        var category = item.getProduct().getCategory();
                        String categoryId = category.getId().toString();

                        CategoryData categoryData = categoryDataMap.computeIfAbsent(categoryId,
                                k -> new CategoryData());
                        categoryData.categoryId = categoryId;
                        categoryData.categoryName = category.getName();
                        categoryData.imageUrl = category.getImageUrl();

                        BigDecimal itemRevenue = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                        categoryData.revenue = categoryData.revenue.add(itemRevenue);
                        categoryData.quantitySold += item.getQuantity() != null ? item.getQuantity() : 0;

                        // Add product data
                        String productId = item.getProduct().getId().toString();
                        ProductData productData = categoryData.products.computeIfAbsent(productId,
                                k -> new ProductData());
                        productData.productId = productId;
                        productData.productName = item.getProduct().getName();
                        productData.price = item.getProduct().getPrice();
                        productData.imageUrl = getProductImageUrl(item.getProduct());
                        productData.quantitySold += item.getQuantity() != null ? item.getQuantity() : 0;
                        productData.revenue = productData.revenue.add(itemRevenue);
                    }
                }
            }
        }

        // Build response with percentages
        List<AnalyticsDTO.CategoryRevenueDetail> categories = new ArrayList<>();
        int colorIndex = 0;

        // Calculate total category revenue (sum of all categories)
        // This ensures percentages add up to 100% even if some items don't have
        // categories
        BigDecimal totalCategoryRevenue = categoryDataMap.values().stream()
                .map(data -> data.revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (CategoryData data : categoryDataMap.values()) {
            // Use totalCategoryRevenue as denominator to ensure sum = 100%
            double percentage = totalCategoryRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? data.revenue.divide(totalCategoryRevenue, 4, RoundingMode.HALF_UP).doubleValue() * 100
                    : 0.0;

            // Convert products map to list
            List<AnalyticsDTO.ProductRevenueDetail> products = data.products.values().stream()
                    .sorted((a, b) -> b.revenue.compareTo(a.revenue))
                    .map(p -> AnalyticsDTO.ProductRevenueDetail.builder()
                            .productId(p.productId)
                            .productName(p.productName)
                            .revenue(p.revenue)
                            .quantitySold(p.quantitySold)
                            .price(p.price)
                            .imageUrl(p.imageUrl)
                            .build())
                    .collect(Collectors.toList());

            categories.add(AnalyticsDTO.CategoryRevenueDetail.builder()
                    .categoryId(data.categoryId)
                    .categoryName(data.categoryName)
                    .revenue(data.revenue)
                    .percentage(percentage)
                    .colorHex(CATEGORY_COLORS[colorIndex % CATEGORY_COLORS.length])
                    .quantitySold(data.quantitySold)
                    .imageUrl(data.imageUrl)
                    .products(products)
                    .build());
            colorIndex++;
        }

        // Sort by revenue
        categories.sort((a, b) -> b.getRevenue().compareTo(a.getRevenue()));

        return AnalyticsDTO.DailyAnalyticsDetailResponse.builder()
                .date(date)
                .totalRevenue(totalRevenue)
                .orderCount(orderCount)
                .categories(categories)
                .message("Daily analytics detail retrieved successfully")
                .build();
    }

    // ==================== Helper Methods ====================

    private List<Order> getCompletedOrdersInRange(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED)
                .filter(o -> o.getCreatedAt() != null)
                .filter(o -> !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }

    private BigDecimal calculateTotalRevenue(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getFinalAmount() != null ? o.getFinalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Double calculatePercentChange(BigDecimal previous, BigDecimal current) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        return current.subtract(previous)
                .divide(previous, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    // ==================== Date Helper Methods ====================

    private record DateRange(LocalDateTime start, LocalDateTime end, int days) {
    }

    private DateRange parseDateRange(String range) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start;
        int days;

        if (range.startsWith("custom:")) {
            try {
                String[] parts = range.substring(7).split(",");
                if (parts.length == 2) {
                    LocalDate startDate = LocalDate.parse(parts[0]);
                    LocalDate endDate = LocalDate.parse(parts[1]);
                    // Start of day for start date, End of day for end date
                    start = startDate.atStartOfDay();
                    end = endDate.atTime(23, 59, 59);
                    days = (int) java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                    return new DateRange(start, end, days);
                }
            } catch (Exception e) {
                log.error("Failed to parse custom date range: {}", range, e);
                // Fallback to 7 days
            }
        }

        // Standard relative ranges
        days = switch (range.toLowerCase()) {
            case "7d", "week" -> 7;
            case "30d", "month" -> 30;
            case "90d", "quarter" -> 90;
            case "365d", "year" -> 365;
            default -> 7; // Default fallback
        };
        start = end.minusDays(days);
        return new DateRange(start, end, days);
    }

    private String getDayLabel(LocalDate date, long totalDays, int index) {
        if (totalDays <= 8) {
            // Short range (One week): Show Day of Week (T2, T3...)
            return switch (date.getDayOfWeek()) {
                case MONDAY -> "T2";
                case TUESDAY -> "T3";
                case WEDNESDAY -> "T4";
                case THURSDAY -> "T5";
                case FRIDAY -> "T6";
                case SATURDAY -> "T7";
                case SUNDAY -> "CN";
            };
        } else if (totalDays <= 31) {
            // Medium range (Month): Show date every 5 days
            if (index % 5 == 0) {
                return String.format("%02d/%02d", date.getDayOfMonth(), date.getMonthValue());
            }
            return "";
        } else {
            // Long range (Quarter/Year): Show Month label only on 1st of month or first
            // item
            if (date.getDayOfMonth() == 1 || index == 0) {
                return "Thg " + date.getMonthValue();
            }
            return "";
        }
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        return String.format("%02d Th%02d - %02d Th%02d",
                start.getDayOfMonth(), start.getMonthValue(),
                end.getDayOfMonth(), end.getMonthValue());
    }

    private String getProductImageUrl(pandq.domain.models.product.Product product) {
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            return product.getImages().get(0).getImageUrl();
        }
        return null;
    }

    // Internal helper classes
    private static class ProductSalesData {
        String productId;
        String productName;
        String imageUrl;
        BigDecimal price = BigDecimal.ZERO;
        long quantitySold = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
    }

    private static class CategorySalesData {
        String categoryId;
        String categoryName;
        BigDecimal revenue = BigDecimal.ZERO;
        long quantitySold = 0;
        String imageUrl;
    }

    // Helper classes for daily analytics detail
    private static class CategoryData {
        String categoryId;
        String categoryName;
        BigDecimal revenue = BigDecimal.ZERO;
        long quantitySold = 0;
        String imageUrl;
        Map<String, ProductData> products = new HashMap<>();
    }

    private static class ProductData {
        String productId;
        String productName;
        BigDecimal revenue = BigDecimal.ZERO;
        long quantitySold = 0;
        BigDecimal price = BigDecimal.ZERO;
        String imageUrl;
    }
}
