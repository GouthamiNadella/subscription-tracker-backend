package com.example.subscriptiontracker.controller;

import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.net.Webhook;
import com.stripe.exception.SignatureVerificationException;
import org.springframework.http.HttpStatus;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;

import com.example.subscriptiontracker.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookController {
    private final SubscriptionService subscriptionService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;


    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) {
        String payload;
        String sigHeader = request.getHeader("Stripe-Signature");

        log.info("\uD83D\uDD14 Received Stripe webhook with signature: {}", sigHeader);

        try {
            payload = request.getReader().lines()
                    .collect(Collectors.joining(System.lineSeparator()));
            log.debug("Webhook payload received, length: {} bytes", payload.length());

        } catch (IOException e) {
            log.error("❌ Error reading webhook request body: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error reading request body");
        }

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("✅ Webhook signature verified successfully");
        } catch (SignatureVerificationException e) {
            log.error("❌ Invalid webhook signature! Possible attack attempt: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        } catch (Exception e) {
            log.error("❌ Error parsing webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error parsing webhook");
        }

        log.info("📬 Processing Stripe event: {} with ID: {}", event.getType(), event.getId());

        try {
            switch (event.getType()) {
                case "invoice.payment_succeeded":
                    handlePaymentSucceeded(event);
                    break;

                case "invoice.payment_failed":
                    handlePaymentFailed(event);
                    break;

                case "customer.subscription.updated":
                    handleSubscriptionUpdated(event);
                    break;

                case "customer.subscription.deleted":
                    handleSubscriptionDeleted(event);
                    break;

                case "invoice.upcoming":
                    handleUpcomingInvoice(event);
                    break;

                default:
                    log.info("ℹ️ Unhandled event type: {}", event.getType());
            }

            log.info("✅ Successfully processed webhook event: {}", event.getType());
            return ResponseEntity.ok("Webhook processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing webhook event {}: {}", event.getType(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook");
        }
    }

    private void handlePaymentSucceeded(Event event) {
        log.info("✅ Processing payment succeeded event");

        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);

            if(invoice != null) {
                String stripeSubscriptionId = getSubscriptionId(invoice);

                BigDecimal amount = BigDecimal.valueOf(invoice.getAmountPaid() / 100.0);
                String currency = invoice.getCurrency().toUpperCase();

                log.info("💰 Payment succeeded: ${} {} for subscription {}",
                        amount, currency, stripeSubscriptionId);

                subscriptionService.processPaymentEvent(
                        stripeSubscriptionId,
                        "PAYMENT_SUCCESS",
                        amount,
                        currency,
                        event.getId()
                );

                log.info("✅ Successfully processed payment success for subscription: {}", stripeSubscriptionId);
            } else {
                log.warn("⚠️ Payment succeeded event missing invoice or subscription data");
            }
        } catch (Exception e) {
            log.error("❌ Error processing payment succeeded webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handlePaymentFailed(Event event) {
        log.warn("❌ Processing payment failed event");

        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);

            if (invoice != null) {
                String stripeSubscriptionId = getSubscriptionId(invoice);

                // Convert cents to dollars
                BigDecimal amount = BigDecimal.valueOf(invoice.getAmountDue() / 100.0);
                String currency = invoice.getCurrency().toUpperCase();

                log.warn("💳 Payment FAILED: ${} {} for subscription {}",
                        amount, currency, stripeSubscriptionId);

                // Record the failure and send alert to user
                subscriptionService.processPaymentEvent(
                        stripeSubscriptionId,
                        "PAYMENT_FAILED",
                        amount,
                        currency,
                        event.getId()
                );

                log.info("✅ Successfully processed payment failure for subscription: {}", stripeSubscriptionId);

            } else {
                log.warn("⚠️ Payment failed event missing invoice or subscription data");
            }

        } catch (Exception e) {
            log.error("❌ Error processing payment failed webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleSubscriptionUpdated(Event event) {
        log.info("\uD83D\uDD04 Processing subscription updated event");

        try {
            Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);

            if(stripeSubscription != null) {
                String subscriptionId = stripeSubscription.getId();

                if(stripeSubscription.getItems() != null && !stripeSubscription.getItems().getData().isEmpty()) {
                    Long newPriceAmount = stripeSubscription.getItems()
                            .getData()
                            .get(0)
                            .getPrice()
                            .getUnitAmount();

                    if(newPriceAmount != null) {
                        BigDecimal newPrice = BigDecimal.valueOf(newPriceAmount / 100.0);

                        log.info("\uD83D\uDCB0 Price detected in subscription update: ${} for subscription {}",
                                newPrice, subscriptionId);

                        subscriptionService.handlePriceChangeFromWebhook(subscriptionId, newPrice);
                    }
                }

                log.info("✅ Successfully processed subscription update for: {}", subscriptionId);
            } else {
                log.warn("⚠️ Subscription updated event missing subscription data");
            }
        } catch (Exception e) {
            log.error("❌ Error processing subscription updated webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleSubscriptionDeleted(Event event) {
        log.info("\uD83D\uDDD1\uFE0F Processing subscription deleted event");

        try {
            Subscription stripeSubscription = (Subscription) event.getDataObjectDeserializer().getObject().orElse(null);

            if (stripeSubscription != null) {
                String subscriptionId = stripeSubscription.getId();

                log.info("\uD83D\uDDD1\uFE0F Subscription cancelled: {}", subscriptionId);

                subscriptionService.handleStripeSubscriptionCancellation(subscriptionId);

                log.info("✅ Successfully processed subscription cancellation for: {}", subscriptionId);
            } else {
                log.warn("⚠️ Subscription deleted event missing subscription data");
            }
        } catch (Exception e) {
            log.error("❌ Error processing subscription deleted webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void handleUpcomingInvoice(Event event) {
        log.info("\uD83D\uDCC5 Processing upcoming invoice event");

        try {
            Invoice invoice = (Invoice) event.getDataObjectDeserializer().getObject().orElse(null);

            if(invoice != null) {
                String subscriptionId = getSubscriptionId(invoice);
                BigDecimal renewalAmount = BigDecimal.valueOf(invoice.getAmountDue() / 100.0);

                log.info("📅 Upcoming renewal detected: ${} for subscription {}",
                        renewalAmount, subscriptionId);

                subscriptionService.handleUpcomingRenewal(subscriptionId, renewalAmount);

                log.info("✅ Successfully processed upcoming invoice for: {}", subscriptionId);
            } else {
                log.warn("⚠️ Upcoming invoice event missing invoice or subscription data");
            }
        } catch (Exception e) {
            log.error("❌ Error processing upcoming invoice webhook: {}", e.getMessage(), e);
            throw e;
        }
    }

    private String getSubscriptionId(Invoice invoice) {
        try {
            Object subscription = invoice.getSubscription();
            if (subscription == null) {
                return null;
            }
            if (subscription instanceof String) {
                return (String) subscription;
            }
            return subscription.toString();
        } catch (Exception e) {
            log.error("Error extracting subscription ID from invoice: {}", e.getMessage());
            return null;
        }
    }

    @GetMapping("/test")
    public ResponseEntity<String> testWebhook() {
        log.info("\uD83E\uDDEA Webhook test endpoint called");

        boolean webhookConfigured = webhookSecret != null && !webhookSecret.isEmpty();

        String response = String.format(
                "🔔 Webhook Endpoint Status\n\n" +
                "✅ Endpoint Active: YES\n" +
                        "🔐 Webhook Secret Configured: %s\n" +
                        "📡 Endpoint URL: /api/webhooks/stripe\n" +
                        "⏰ Timestamp: %s\n\n" +
                        "📝 Next Steps:\n" +
                        "1. Get webhook secret from Stripe Dashboard\n" +
                        "2. Add to application.properties: stripe.webhook.secret=whsec_...\n" +
                        "3. Use Stripe CLI to test: stripe listen --forward-to localhost:8080/api/webhooks/stripe\n" +
                        "4. Trigger test event: stripe trigger invoice.payment_succeeded",

                webhookConfigured ? "YES ✅" : "NO ❌ (Add to application.properties)",
                java.time.LocalDateTime.now()
        );

        return ResponseEntity.ok(response);
    }
}
