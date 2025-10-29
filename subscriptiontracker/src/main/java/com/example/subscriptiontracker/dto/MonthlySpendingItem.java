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
public class MonthlySpendingItem {
    private String month;
    private BigDecimal spending;
    private int subscriptionCount;
}
