package com.example.subscriptiontracker.controller;

import com.example.subscriptiontracker.dto.CreateSubscriptionRequest;
import com.example.subscriptiontracker.dto.SubscriptionResponse;
import com.example.subscriptiontracker.dto.UpdateSubscriptionRequest;
import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.model.SubscriptionStatus;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import com.example.subscriptiontracker.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/api/subscriptions")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
@Slf4j
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<SubscriptionResponse>> getUserSubscriptions(@PathVariable Long userId) {
        log.info("Fetching subscriptions for user: {}", userId);

        try{
            //List<Subscription> subscriptions = subscriptionService.getUserSubscriptions(userId);
            List<Subscription> subscriptions = subscriptionRepository
                    .findByUserIdAndStatusNot(userId, SubscriptionStatus.CANCELLED);
            List<SubscriptionResponse> response = subscriptions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Found {} subscriptions for user {}", response.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch subscriptions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<SubscriptionResponse>> getActiveSubscriptions(@PathVariable Long userId) {
        log.info("Fetching active subscriptions for user: {}", userId);

        try {
            List<Subscription> subscriptions = subscriptionService.getActiveSubscriptions(userId);

            List<SubscriptionResponse> response = subscriptions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Found {} active subscriptions for user {}", response.size(), userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch active subscriptions for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) {
        log.info("Creating subscription for user {} - Service: {}", request.getUserId(), request.getName());

        try {
            Subscription subscription = subscriptionService.createSubscription(
                    request.getUserId(),
                    request.getName(),
                    request.getPlanName(),
                    request.getPrice(),
                    request.getCategory(),
                    request.getCard(),
                    request.getNextRenewalDate()
            );

            SubscriptionResponse response = convertToResponse(subscription);

            log.info("Successfully created subscription with ID: {}", subscription.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create subscription: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{subscriptionId}")
    public ResponseEntity<SubscriptionResponse> updateSubscription(
            @PathVariable Long subscriptionId,
            @Valid @RequestBody UpdateSubscriptionRequest request) {
        log.info("Updating subscription with ID: {}", subscriptionId);

        try {
            Subscription subscription = subscriptionService.updateSubscription(
                    subscriptionId,
                    request.getName(),
                    request.getPlanName(),
                    request.getPrice(),
                    request.getCategory(),
                    request.getCard(),
                    request.getNextRenewalDate()
            );
            SubscriptionResponse response = convertToResponse(subscription);

            log.info("Successfully updated subscription with ID: {}", subscriptionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to update subscription {}: {}", subscriptionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{subscriptionId}")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long subscriptionId){
        log.info("Cancelling subscription with ID: {}", subscriptionId);

        try {
            subscriptionService.cancelSubscription(subscriptionId);

            log.info("Successfully cancelled subscription with ID: {}", subscriptionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.info("Failed to cancel subscription {}: {}", subscriptionId, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /*@GetMapping("/renewals/upcoming")
    public ResponseEntity<List<SubscriptionResponse>> getUpcomingRenewals(@RequestParam(defaultValue = "7") int days) {
        log.info("Fetching subscriptions needing reminders in {} days", days);

        try {
            List<Subscription> subscriptions = subscriptionService.getSubscriptionsNeedingRemainders(days);

            List<SubscriptionResponse> response = subscriptions.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());

            log.info("Found {} subscriptions needing reminders", response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to fetch upcoming renewals: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }*/

    private SubscriptionResponse convertToResponse(Subscription subscription) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUser().getId())
                .name(subscription.getName())
                .planName(subscription.getPlanName())
                .price(subscription.getPrice())
                .currency(subscription.getCurrency())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .nextRenewalDate(subscription.getNextRenewalDate())
                .status(subscription.getStatus())
                .category(subscription.getCategory())
                .card(subscription.getCard())
                .autoPayment(subscription.isAutoPayment())
                .notificationsEnabled(subscription.isNotificationsEnabled())
                .description(subscription.getDescription())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }
}
