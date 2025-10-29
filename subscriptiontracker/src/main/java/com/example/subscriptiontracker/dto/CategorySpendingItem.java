package com.example.subscriptiontracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpendingItem {

    private String category;
    private BigDecimal totalSpending;
    private int subscriptionCount;
    private double percentage;
}
