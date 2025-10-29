package com.example.subscriptiontracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String planName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDate startDate;
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    private String category;

    @Column(nullable = false)
    private LocalDateTime nextRenewalDate;

    @Builder.Default
    private boolean autoPayment = false;

    private String card; // card name Ex: chase credit

    @Builder.Default
    private boolean notificationsEnabled = true;

    // Essential webhook & tracking field
    private String stripeSubscriptionId; // For Stripe webhook integration

    @Column(length = 3)
    @Builder.Default
    private String currency = "USD";

    private String webhookEndpoint; // For custom webhook sources

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PriceHistory> priceHistory = new ArrayList<>();

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<PaymentEvent> paymentEvents = new ArrayList<>();

    public void addPriceChange(BigDecimal oldPrice, BigDecimal newPrice, String reason) {
        PriceHistory history = PriceHistory.builder()
                .subscription(this)
                .oldPrice(oldPrice)
                .newPrice(newPrice)
                .reason(reason)
                .build();
        this.priceHistory.add(history);
    }

    public void addPaymentEvent(String eventType, BigDecimal amount, String description) {
        PaymentEvent event = PaymentEvent.builder()
                .subscription(this)
                .eventType(eventType)
                .amount(amount)
                .currency(this.currency)
                .description(description)
                .build();
        this.paymentEvents.add(event);
    }

    public boolean isExpiringSoon(int days) {
        return nextRenewalDate.isBefore(LocalDateTime.now().plusDays(days));
    }

    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE;
    }

    public BigDecimal getYearlyPrice() {
        return price.multiply(BigDecimal.valueOf(12));
    }
}
