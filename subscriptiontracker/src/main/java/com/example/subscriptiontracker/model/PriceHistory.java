package com.example.subscriptiontracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "old_price", precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", precision = 10, scale = 2, nullable = false)
    private BigDecimal newPrice;

    @CreationTimestamp
    @Column(name = "change_date", nullable = false, updatable = false)
    private LocalDateTime changeDate;

    @Column(length = 500)
    private String reason;

    public boolean isPriceIncrease() {
        if(oldPrice == null || newPrice == null) {
            return false;
        }
        return newPrice.compareTo(oldPrice) > 0;
    }

    public BigDecimal getDifference() {
        if(oldPrice == null || newPrice == null) {
            return BigDecimal.ZERO;
        }
        return newPrice.subtract(oldPrice);
    }

    public double getPercentageChange() {
        if(oldPrice == null || newPrice == null || oldPrice.compareTo(BigDecimal.ZERO) == 0) {
            return 0.0;
        }

        BigDecimal difference = newPrice.subtract(oldPrice);
        return difference.divide(oldPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
