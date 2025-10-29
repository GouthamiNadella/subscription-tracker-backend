package com.example.subscriptiontracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private Long userId;

    private int totalSubscriptions;
    private int activeSubscriptions;
    private int cancelledSubscriptions;

    private BigDecimal totalMonthlySpending;
    private BigDecimal totalYearlySpending;
    private BigDecimal averageSubscriptionCost;

    private int upcomingRenewals;
    private int upcomingRenewalsThisMonth;

    private String mostExpensiveSubscription;
    private BigDecimal mostExpensiveAmount;
    private String newestSubscription;

    private List<RecentActivityItem> recentActivity;

    private List<CategorySpendingItem> categoryBreakdown;
}
