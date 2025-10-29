package com.example.subscriptiontracker.controller;

import com.example.subscriptiontracker.dto.DashboardResponse;
import com.example.subscriptiontracker.dto.SpendingAnalyticsResponse;
import com.example.subscriptiontracker.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard/{userId}")
    public ResponseEntity<DashboardResponse> getDashboard(@PathVariable Long userId) {
        log.info("\uD83D\uDCCA Dashboard request for user: {}", userId);

        try {
            DashboardResponse dashboard = analyticsService.generateDashboard(userId);

            log.info("✅ Dashboard generated: {} active subscriptions, ${} monthly",
                    dashboard.getActiveSubscriptions(),
                    dashboard.getTotalMonthlySpending());

            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            log.error("❌ Failed to generate dashboard for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/spending-trends/{userId}")
    public ResponseEntity<SpendingAnalyticsResponse> getSpendingTrends(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "12") int months){

        log.info("📈 Spending trends request for user {} over {} months", userId, months);

        try {
            SpendingAnalyticsResponse analytics = analyticsService.getSpendingAnalytics(userId, months);

            log.info("✅ Spending trends generated: {} data points, trend: {}",
                    analytics.getMonthlyData().size(),
                    analytics.getTrendDirection());

            return ResponseEntity.ok(analytics);
        } catch (Exception e) {
            log.error("❌ Failed to generate spending trends for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/categories/{userId}")
    public ResponseEntity<SpendingAnalyticsResponse> getCategoryBreakdown(@PathVariable Long userId) {
        log.info("🥧 Category breakdown request for user: {}", userId);

        try {
            SpendingAnalyticsResponse analytics = analyticsService.getCategoryBreakdown(userId);

            log.info("✅ Category breakdown generated: {} categories",
                    analytics.getCategoryData() != null ? analytics.getCategoryData().size() : 0);

            return ResponseEntity.ok(analytics);

        } catch (Exception e) {
            log.error("❌ Failed to generate category breakdown for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
