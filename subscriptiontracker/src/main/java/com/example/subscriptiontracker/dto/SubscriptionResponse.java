package com.example.subscriptiontracker.dto;

import com.example.subscriptiontracker.model.SubscriptionStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private String name;
    private String planName;
    private BigDecimal price;
    private String currency;
    private LocalDate startDate;
    private LocalDate endDate; // For fixed-term subscriptions
    private LocalDateTime nextRenewalDate;
    private SubscriptionStatus status;
    private String category;
    private String card;
    private boolean autoPayment;
    private boolean notificationsEnabled;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public long getDaysUntilRenewal() {
        if (nextRenewalDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(
                LocalDateTime.now(),
                nextRenewalDate
        );
    }

    public BigDecimal getYearlyPrice() {
        if (price == null) return BigDecimal.ZERO;
        return price.multiply(BigDecimal.valueOf(12));
    }

    public boolean isExpiringSoon() {
        return getDaysUntilRenewal() <= 7 && getDaysUntilRenewal() > 0;
    }
}
