package com.example.subscriptiontracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "event_type", nullable = false)
    private String eventType; // PAYMENT_SUCCESS, PAYMENT_FAILED, REFUND

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @CreationTimestamp
    @Column(name = "event_date", nullable = false, updatable = false)
    private LocalDateTime eventDate;

    @Column(name = "stripe_event_id")
    private String stripeEventId;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    private boolean processed = false;

    public boolean isFailure() {
        return "PAYMENT_FAILED".equals(eventType) ||
                "CHARGEBACK".equals(eventType);
    }

    public boolean isSuccess() {
        return "PAYMENT_SUCCESS".equals(eventType);
    }

    public String getDisplayDescription() {
        String action = isSuccess() ? "succeeded" : "failed";
        return String.format("Payment of $%.2f %s on %s",
                amount,
                action,
                eventDate.toLocalDate());
    }
}
