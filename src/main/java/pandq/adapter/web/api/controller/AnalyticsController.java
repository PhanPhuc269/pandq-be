package pandq.adapter.web.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pandq.adapter.web.api.dtos.AnalyticsDTO;
import pandq.application.services.AnalyticsService;

/**
 * Analytics Controller - REST API endpoints for sales analytics.
 * Following Clean Architecture - Controller belongs in Adapter layer.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get sales overview KPIs
     * 
     * @param range Date range: 7d, 30d, 90d, 365d, week, month, quarter, year
     */
    @GetMapping("/sales-overview")
    public ResponseEntity<AnalyticsDTO.SalesOverviewResponse> getSalesOverview(
            @RequestParam(defaultValue = "30d") String range) {
        log.info("GET /api/v1/analytics/sales-overview?range={}", range);
        return ResponseEntity.ok(analyticsService.getSalesOverview(range));
    }

    /**
     * Get revenue chart data (daily breakdown)
     * 
     * @param range Date range for the chart
     */
    @GetMapping("/revenue-chart")
    public ResponseEntity<AnalyticsDTO.RevenueChartResponse> getRevenueChart(
            @RequestParam(defaultValue = "7d") String range) {
        log.info("GET /api/v1/analytics/revenue-chart?range={}", range);
        return ResponseEntity.ok(analyticsService.getRevenueChart(range));
    }

    /**
     * Get top selling products
     * 
     * @param limit Number of products to return (default 4)
     */
    @GetMapping("/top-products")
    public ResponseEntity<AnalyticsDTO.TopProductsResponse> getTopProducts(
            @RequestParam(defaultValue = "4") int limit,
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(defaultValue = "quantity") String sortBy) {
        log.info("GET /api/v1/analytics/top-products?limit={}&range={}&sortBy={}", limit, range, sortBy);
        return ResponseEntity.ok(analyticsService.getTopProducts(limit, range, sortBy));
    }

    /**
     * Get category sales distribution
     */
    @GetMapping("/category-sales")
    public ResponseEntity<AnalyticsDTO.CategorySalesResponse> getCategorySales(
            @RequestParam(defaultValue = "30d") String range,
            @RequestParam(defaultValue = "revenue") String sortBy) {
        log.info("GET /api/v1/analytics/category-sales?range={}&sortBy={}", range, sortBy);
        return ResponseEntity.ok(analyticsService.getCategorySales(range, sortBy));
    }

    /**
     * Get full analytics data in one request (combined response)
     * 
     * @param range Date range for time-based analytics
     */
    @GetMapping("/full")
    public ResponseEntity<AnalyticsDTO.FullAnalyticsResponse> getFullAnalytics(
            @RequestParam(defaultValue = "30d") String range) {
        log.info("GET /api/v1/analytics/full?range={}", range);
        return ResponseEntity.ok(analyticsService.getFullAnalytics(range));
    }

    /**
     * Get detailed analytics for a specific date
     * 
     * @param date Date in format yyyy-MM-dd (e.g. 2026-01-06)
     */
    @GetMapping("/daily/{date}")
    public ResponseEntity<AnalyticsDTO.DailyAnalyticsDetailResponse> getDailyAnalyticsDetail(
            @PathVariable String date) {
        log.info("GET /api/v1/analytics/daily/{}", date);
        try {
            java.time.LocalDate localDate = java.time.LocalDate.parse(date);
            return ResponseEntity.ok(analyticsService.getDailyAnalyticsDetail(localDate));
        } catch (java.time.format.DateTimeParseException e) {
            log.error("Invalid date format: {}", date, e);
            return ResponseEntity.badRequest().build();
        }
    }
}
