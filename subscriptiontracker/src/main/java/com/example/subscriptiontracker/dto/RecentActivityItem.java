package com.example.subscriptiontracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentActivityItem {
    private String type;
    private String description;
    private String subscriptionName;
    private LocalDateTime timestamp;
    private String severity;
}
