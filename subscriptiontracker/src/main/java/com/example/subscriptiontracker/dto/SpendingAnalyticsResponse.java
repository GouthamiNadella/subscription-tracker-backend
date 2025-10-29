package com.example.subscriptiontracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpendingAnalyticsResponse {
    private Long userId;
    private int periodMonths;

    private List<MonthlySpendingItem> monthlyData;
    private List<CategorySpendingItem> categoryData;

    private String trendDirection;
    private Double trendPercentage;
}
