package com.example.subscriptiontracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubscriptionRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Subscription name is required")
    private String name; // Netflix, Spotify, etc.

    private String planName; // Premium, Basic, etc. (optional)

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    private String category; // Entertainment, Productivity, etc.

    private String card; // Chase Freedom, Visa Debit, etc.

    @NotNull(message = "Next renewal date is required")
    @Future(message = "Next renewal date must be in the future")
    private LocalDateTime nextRenewalDate;

}
