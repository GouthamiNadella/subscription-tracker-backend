package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.dto.*;
import com.example.subscriptiontracker.model.PaymentEvent;
import com.example.subscriptiontracker.model.PriceHistory;
import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.model.SubscriptionStatus;
import com.example.subscriptiontracker.repository.PaymentEventRepository;
import com.example.subscriptiontracker.repository.PriceHistoryRepository;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.*;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {
    private final SubscriptionRepository subscriptionRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PriceHistoryRepository priceHistoryRepository;

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy");

    public DashboardResponse generateDashboard(Long userId) {
        log.info("\uD83D\uDCCA Generating dashboard for user: {}", userId);

        List<Subscription> allSubscriptions = subscriptionRepository.findByUserId(userId);
        List<Subscription> activeSubscriptions = allSubscriptions.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .collect(Collectors.toList());

        BigDecimal totalMonthly = activeSubscriptions.stream()
                .map(Subscription::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalYearly = totalMonthly.multiply(BigDecimal.valueOf(12));

        BigDecimal averageCost = activeSubscriptions.isEmpty() ?
                BigDecimal.ZERO :
                totalMonthly.divide(BigDecimal.valueOf(activeSubscriptions.size()), 2, RoundingMode.HALF_UP);

        LocalDateTime nextWeek = LocalDateTime.now().plusDays(7);
        int upcomingRenewals = (int) activeSubscriptions.stream()
                .filter(s -> s.getNextRenewalDate().isBefore(nextWeek))
                .count();

        LocalDateTime endOfMonth = LocalDateTime.now()
                .withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth())
                .withHour(23).withMinute(59);
        int upcomingRenewalsThisMonth = (int) activeSubscriptions.stream()
                .filter(s -> s.getNextRenewalDate().isBefore(endOfMonth))
                .count();

        String mostExpensive = "None";
        BigDecimal mostExpensiveAmount = BigDecimal.ZERO;
        for (Subscription sub : activeSubscriptions) {
            if (sub.getPrice().compareTo(mostExpensiveAmount) > 0) {
                mostExpensiveAmount = sub.getPrice();
                mostExpensive = sub.getName();
            }
        }

        String newestSubscription = allSubscriptions.stream()
                .max(Comparator.comparing(Subscription::getCreatedAt))
                .map(Subscription::getName)
                .orElse("None");

        List<RecentActivityItem> recentActivity = getRecentActivity(userId);

        List<CategorySpendingItem> categoryBreakdown = getCategoryBreakdownData(activeSubscriptions, totalMonthly);

        return DashboardResponse.builder()
                .userId(userId)
                .totalSubscriptions(allSubscriptions.size())
                .activeSubscriptions(activeSubscriptions.size())
                .cancelledSubscriptions((int) allSubscriptions.stream()
                        .filter(s -> s.getStatus() == SubscriptionStatus.CANCELLED)
                        .count())
                .totalMonthlySpending(totalMonthly)
                .totalYearlySpending(totalYearly)
                .averageSubscriptionCost(averageCost)
                .upcomingRenewals(upcomingRenewals)
                .upcomingRenewalsThisMonth(upcomingRenewalsThisMonth)
                .mostExpensiveSubscription(mostExpensive)
                .mostExpensiveAmount(mostExpensiveAmount)
                .newestSubscription(newestSubscription)
                .recentActivity(recentActivity)
                .categoryBreakdown(categoryBreakdown)
                .build();
    }

    public SpendingAnalyticsResponse getSpendingAnalytics(Long userId, int months) {
        log.info("📈 Generating spending analytics for user {} over {} months", userId, months);

        List<MonthlySpendingItem> monthlyData = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();

        for (int i = months - 1; i >= 0; i--) {
            LocalDateTime monthStart = endDate.minusMonths(i)
                    .withDayOfMonth(1)
                    .withHour(0).withMinute(0).withSecond(0);
            LocalDateTime monthEnd = monthStart.plusMonths(1).minusSeconds(1);

            List<Subscription> monthSubscriptions = subscriptionRepository
                    .findByUserIdAndDateRange(userId, monthStart, monthEnd);

            BigDecimal monthlySpending = monthSubscriptions.stream()
                    .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                    .map(Subscription::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            monthlyData.add(MonthlySpendingItem.builder()
                    .month(monthStart.format(MONTH_FORMATTER))
                    .spending(monthlySpending)
                    .subscriptionCount(monthSubscriptions.size())
                    .build());
        }
        String trendDirection = calculateTrend(monthlyData);
        double trendPercentage = calculateTrendPercentage(monthlyData);

        return SpendingAnalyticsResponse.builder()
                .userId(userId)
                .periodMonths(months)
                .monthlyData(monthlyData)
                .trendDirection(trendDirection)
                .trendPercentage(trendPercentage)
                .build();
    }

    public SpendingAnalyticsResponse getCategoryBreakdown(Long userId) {
        log.info("🥧 Generating category breakdown for user: {}", userId);

        List<Subscription> activeSubscriptions = subscriptionRepository
                .findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        BigDecimal totalSpending = activeSubscriptions.stream()
                .map(Subscription::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<CategorySpendingItem> categoryData = getCategoryBreakdownData(activeSubscriptions, totalSpending);

        return SpendingAnalyticsResponse.builder()
                .userId(userId)
                .categoryData(categoryData)
                .build();
    }

    private List<RecentActivityItem> getRecentActivity(Long userId) {
        List<RecentActivityItem> activity = new ArrayList<>();
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        try {
            List<PriceHistory> recentPriceChanges = priceHistoryRepository
                    .findRecentPriceChangesForUser(userId, since);

            for (PriceHistory priceChange : recentPriceChanges.stream().limit(5).collect(Collectors.toList())) {
                String severity = priceChange.getNewPrice().compareTo(priceChange.getOldPrice()) > 0
                        ? "WARNING" : "INFO";

                activity.add(RecentActivityItem.builder()
                        .type("PRICE_CHANGE")
                        .subscriptionName(priceChange.getSubscription().getName())
                        .description(String.format("%s price changed from $%.2f to $%.2f",
                                priceChange.getSubscription().getName(),
                                priceChange.getOldPrice(),
                                priceChange.getNewPrice()))
                        .timestamp(priceChange.getChangeDate())
                        .severity(severity)
                        .build());
            }

            List<PaymentEvent> recentPayments = paymentEventRepository
                    .findRecentPaymentEventsForUser(userId, since);

            for (PaymentEvent payment : recentPayments.stream().limit(5).collect(Collectors.toList())) {
                String severity = "PAYMENT_FAILED".equals(payment.getEventType()) ? "ERROR" : "INFO";

                activity.add(RecentActivityItem.builder()
                        .type(payment.getEventType())
                        .subscriptionName(payment.getSubscription().getName())
                        .description(String.format("%s: $%.2f for %s",
                                payment.getEventType().replace("_", " ").toLowerCase(),
                                payment.getAmount(),
                                payment.getSubscription().getName()))
                        .timestamp(payment.getEventDate())
                        .severity(severity)
                        .build());
            }
        } catch (Exception e) {
            log.error("Error fetching recent activity for user {}: {}", userId, e.getMessage());
        }

        return activity.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<CategorySpendingItem> getCategoryBreakdownData(List<Subscription> subscriptions, BigDecimal totalSpending) {
        Map<String, List<Subscription>> categoryGroups = subscriptions.stream()
                .collect(Collectors.groupingBy(s ->
                        s.getCategory() != null ? s.getCategory() : "Other"));

        return categoryGroups.entrySet().stream()
                .map(entry -> {
                    String category = entry.getKey();
                    List<Subscription> categorySubs = entry.getValue();

                    BigDecimal categorySpending = categorySubs.stream()
                            .map(Subscription::getPrice)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    double percentage = totalSpending.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                            categorySpending.divide(totalSpending, 4, RoundingMode.HALF_UP)
                                    .multiply(BigDecimal.valueOf(100)).doubleValue();

                    return CategorySpendingItem.builder()
                            .category(category)
                            .totalSpending(categorySpending)
                            .subscriptionCount(categorySubs.size())
                            .percentage(percentage)
                            .build();
                })
                .sorted((a, b) -> b.getTotalSpending().compareTo(a.getTotalSpending()))
                .collect(Collectors.toList());
    }

    private String calculateTrend(List<MonthlySpendingItem> monthlyData) {
        if (monthlyData.size() < 2) return "STABLE";

        BigDecimal first = monthlyData.get(0).getSpending();
        BigDecimal last = monthlyData.get(monthlyData.size() - 1).getSpending();

        int comparison = last.compareTo(first);
        if (comparison > 0) return "INCREASING";
        if (comparison < 0) return "DECREASING";
        return "STABLE";
    }

    private double calculateTrendPercentage(List<MonthlySpendingItem> monthlyData) {
        if (monthlyData.size() < 2) return 0.0;

        BigDecimal first = monthlyData.get(0).getSpending();
        BigDecimal last = monthlyData.get(monthlyData.size() - 1).getSpending();

        if (first.compareTo(BigDecimal.ZERO) == 0) return 0.0;

        return last.subtract(first)
                .divide(first, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
