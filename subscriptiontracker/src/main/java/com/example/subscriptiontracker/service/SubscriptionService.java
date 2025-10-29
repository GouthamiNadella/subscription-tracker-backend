package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.model.*;
import com.example.subscriptiontracker.repository.*;
import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.model.SubscriptionStatus;
import com.example.subscriptiontracker.repository.SubscriptionRepository;
import com.example.subscriptiontracker.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SubscriptionService {
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PaymentEventRepository paymentEventRepository;

    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getActiveSubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(Long subscriptionId) {
        return subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + subscriptionId));
    }

    public Subscription createSubscription(Long userId, String name, String planName,
                                           BigDecimal price, String category, String card,
                                           LocalDateTime nextRenewalDate) {
        log.info("Creating subscription for user {} - Service: {}", userId, name);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        Subscription subscription = Subscription.builder()
                .user(user)
                .name(name)
                .planName(planName)
                .price(price)
                .category(category)
                .card(card)
                .nextRenewalDate(nextRenewalDate)
                .startDate(java.time.LocalDate.now())
                .status(SubscriptionStatus.ACTIVE)
                .currency("USD")
                .autoPayment(true)
                .notificationsEnabled(true)
                .build();

        Subscription savedSubscription = subscriptionRepository.save(subscription);

        log.info("Subscription saved with ID: {}", savedSubscription.getId());

        PriceHistory initialPrice = PriceHistory.builder()
                .subscription(savedSubscription)
                .oldPrice(BigDecimal.ZERO)
                .newPrice(price)
                .reason("Initial subscription")
                .build();

        priceHistoryRepository.save(initialPrice);
        log.info("Initial price history created for subscription: {}", savedSubscription.getId());

        return savedSubscription;
    }

    public Subscription updateSubscription(Long subscriptionId, String name, String planName,
                                        BigDecimal newPrice, String category, String card,
                                        LocalDateTime nextRenewalDate) {
        log.info("Updating subscription with ID: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + subscriptionId));

        BigDecimal oldPrice = subscription.getPrice();

        subscription.setName(name);
        subscription.setPlanName(planName);
        subscription.setPrice(newPrice);
        subscription.setCategory(category);
        subscription.setCard(card);
        subscription.setNextRenewalDate(nextRenewalDate);

        Subscription updatedSubscription = subscriptionRepository.save(subscription);

        if (!oldPrice.equals(newPrice)) {
            log.info("Price changed for subscription {}: {} -> {}", subscriptionId, oldPrice, newPrice);

            PriceHistory priceChange = PriceHistory.builder()
                    .subscription(updatedSubscription)
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .reason("Manual price update")
                    .build();

            priceHistoryRepository.save(priceChange);
            log.info("Price history record created for subscription: {}", subscriptionId);

            if (newPrice.compareTo(oldPrice) > 0) {
                log.info("Price increased - sending notification");
                notificationService.sendPriceIncreaseNotification(updatedSubscription, oldPrice, newPrice);
            }
        }

        log.info("Successfully updated subscription with ID: {}", subscriptionId);
        return updatedSubscription;
    }

    public void cancelSubscription(Long subscriptionId) {
        log.info("Cancelling subscription with ID: {}", subscriptionId);

        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found with id: " + subscriptionId));

        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        notificationService.sendCancellationConfirmation(subscription);
    }

    public List<Subscription> getSubscriptionsNeedingReminders(int daysAhead) {
        LocalDate targetDate = LocalDate.now().plusDays(daysAhead);
        log.info("🔍 Looking for subscriptions renewing on: {}", targetDate);
        List<Subscription> subscriptions = subscriptionRepository
                .findByNextRenewalDate(targetDate);

        log.info("📊 Found {} subscriptions renewing on {}", subscriptions.size(), targetDate);
        return subscriptions;

        /*LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(daysAhead).plusHours(23).plusMinutes(59);

        List<Subscription> subscriptions = subscriptionRepository
                .findSubscriptionsForRenewalReminder(start, end);

        log.info("Found {} subscriptions needing reminders in {} days", subscriptions.size(), daysAhead);
        return subscriptions;*/
    }

    @Transactional(readOnly = true)
    public List<PriceHistory> getPriceHistory(Long subscriptionId) {
        log.info("Fetching price history for subscription: {}", subscriptionId);
        return priceHistoryRepository.findBySubscriptionIdOrderByChangeDateDesc(subscriptionId);
    }

    @Transactional(readOnly = true)
    public List<PaymentEvent> getPaymentHistory(Long subscriptionId) {
        log.info("Fetching payment history for subscription: {}", subscriptionId);
        return paymentEventRepository.findBySubscriptionIdOrderByEventDateDesc(subscriptionId);
    }

    public void processPaymentEvent(String stripeSubscriptionId, String eventType,
                                    BigDecimal amount, String currency, String stripeEventId) {

        log.info("Processing payment event: {} for Stripe subscription: {}", eventType, stripeSubscriptionId);

        if (paymentEventRepository.findByStripeEventId(stripeEventId).isPresent()) {
            log.warn("Payment event {} already processed, skipping", stripeEventId);
            return;
        }

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for Stripe ID: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        PaymentEvent paymentEvent = PaymentEvent.builder()
                .subscription(subscription)
                .eventType(eventType)
                .amount(amount)
                .currency(currency)
                .stripeEventId(stripeEventId)
                .description("Payment event from Stripe webhook")
                .processed(true)
                .build();

        paymentEventRepository.save(paymentEvent);
        log.info("Payment event record created for subscription: {}", subscription.getId());

        if ("PAYMENT_SUCCESS".equals(eventType)) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);

            subscription.setNextRenewalDate(subscription.getNextRenewalDate().plusMonths(1));
            log.info("Payment successful - subscription {} status updated to ACTIVE", subscription.getId());

        } else if ("PAYMENT_FAILED".equals(eventType)) {
            subscription.setStatus(SubscriptionStatus.PAYMENT_FAILED);

            notificationService.sendPaymentFailedNotification(subscription);
            log.warn("Payment failed - notification sent for subscription {}", subscription.getId());
        }

        subscriptionRepository.save(subscription);
        log.info("Successfully processed payment event: {}", eventType);
    }

    public void handlePriceChangeFromWebhook(String stripeSubscriptionId, BigDecimal newPrice) {
        log.info("Handling price change from webhook for Stripe subscription: {}", stripeSubscriptionId);

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for Stripe ID: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        BigDecimal oldPrice = subscription.getPrice();

        if (!oldPrice.equals(newPrice)) {

            subscription.setPrice(newPrice);
            subscriptionRepository.save(subscription);
            log.info("Subscription {} price updated: {} -> {}", subscription.getId(), oldPrice, newPrice);

            PriceHistory priceChange = PriceHistory.builder()
                    .subscription(subscription)
                    .oldPrice(oldPrice)
                    .newPrice(newPrice)
                    .reason("Price change via Stripe webhook")
                    .build();

            priceHistoryRepository.save(priceChange);
            log.info("Price history record created via webhook for subscription: {}", subscription.getId());

            notificationService.sendPriceIncreaseNotification(subscription, oldPrice, newPrice);
            log.info("Price change notification sent for subscription: {}", subscription.getId());
        } else {
            log.info("Price unchanged for subscription {}, no action needed", subscription.getId());
        }
    }

    public void handleStripeSubscriptionCancellation(String stripeSubscriptionId) {
        log.info("Handling cancellation from webhook for Stripe subscription: {}", stripeSubscriptionId);

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for Stripe ID: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);

        notificationService.sendCancellationConfirmation(subscription);
        log.info("Subscription {} cancelled via webhook", subscription.getId());
    }

    public void handleUpcomingRenewal(String stripeSubscriptionId, BigDecimal renewalAmount) {
        log.info("Handling upcoming renewal for Stripe subscription: {}", stripeSubscriptionId);

        Optional<Subscription> subscriptionOpt =
                subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId);

        if (subscriptionOpt.isEmpty()) {
            log.warn("No subscription found for Stripe ID: {}", stripeSubscriptionId);
            return;
        }

        Subscription subscription = subscriptionOpt.get();

        if (!subscription.getPrice().equals(renewalAmount)) {
            log.info("Price change detected in upcoming renewal - processing");
            handlePriceChangeFromWebhook(stripeSubscriptionId, renewalAmount);
        } else {
            log.info("No price change in upcoming renewal for subscription {}", subscription.getId());
        }
    }
}
