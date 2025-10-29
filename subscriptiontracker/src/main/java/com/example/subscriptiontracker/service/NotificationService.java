package com.example.subscriptiontracker.service;

import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    public void sendRenewalReminder(Subscription subscription, int daysUntilRenewal) {

        User user = subscription.getUser();

        if (!user.isEmailNotifications() || !subscription.isNotificationsEnabled()) {
            log.debug("Skipping renewal reminder for user {} - notifications disabled", user.getId());
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("noreply@subscriptiontracker.com");
            message.setSubject("🔔 Subscription Renewal Reminder - " + subscription.getName());

            String renewalText = daysUntilRenewal == 0 ? "today" :
                    daysUntilRenewal == 1 ? "tomorrow" : daysUntilRenewal + " days";

            String emailBody = String.format(
                    "Hi %s,\n\n" +
                            "Your %s subscription (%s) will renew in %s.\n\n" +
                            "📅 Renewal Date: %s\n" +
                            "💰 Amount: $%.2f %s\n" +
                            "💳 Payment Method: %s\n\n" +
                            "If you want to cancel or modify this subscription, please log into your account.\n\n" +
                            "Manage subscriptions: http://localhost:3000/subscriptions\n\n" +
                            "Best regards,\n" +
                            "Subscription Tracker Team",

                    user.getName(),
                    subscription.getName(),
                    subscription.getPlanName() != null ? subscription.getPlanName() : "Standard Plan",
                    renewalText,
                    subscription.getNextRenewalDate().format(DATE_TIME_FORMATTER),
                    subscription.getPrice(),
                    subscription.getCurrency(),
                    subscription.getCard() != null ? subscription.getCard() : "Default card"
            );

            message.setText(emailBody);

            mailSender.send(message);

            log.info("✅ Successfully sent renewal reminder to {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send renewal reminder to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendPriceIncreaseNotification(Subscription subscription, BigDecimal oldPrice, BigDecimal newPrice) {
        User user = subscription.getUser();

        if(!user.isEmailNotifications()) {
            return;
        }

        log.info("Sending price change notification to {} for subscription: {}",
                user.getEmail(), subscription.getName());

        try {
            BigDecimal monthlyIncrease = newPrice.subtract(oldPrice);
            BigDecimal yearlyIncrease = monthlyIncrease.multiply(BigDecimal.valueOf(12));

            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("noreply@subscriptiontracker.com");
            message.setSubject("⚠️ Price Change Alert - " + subscription.getName());

            String emailBody = String.format(
                    "Hi %s,\n\n" +
                            "We detected a price change for your %s subscription:\n\n" +
                            "📊 Price Change Details:\n" +
                            "   Previous Price: $%.2f %s\n" +
                            "   New Price: $%.2f %s\n" +
                            "   Monthly Increase: +$%.2f\n" +
                            "   Yearly Increase: +$%.2f\n\n" +
                            "📅 This change will be reflected in your next billing cycle on %s.\n\n" +
                            "💡 Pro Tip: Review your subscription to see if you still need this service " +
                            "or if there's a cheaper plan available.\n\n" +
                            "Manage subscriptions: http://localhost:3000/subscriptions\n\n" +
                            "Best regards,\n" +
                            "Subscription Tracker Team",

                    user.getName(),
                    subscription.getName(),
                    oldPrice,
                    subscription.getCurrency(),
                    newPrice,
                    subscription.getCurrency(),
                    monthlyIncrease,
                    yearlyIncrease,
                    subscription.getNextRenewalDate().format(DATE_TIME_FORMATTER) // <--- FIXED: Formatter name
            );

            message.setText(emailBody);
            mailSender.send(message);

            log.info("✅ Successfully sent price change notification to {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send price change notification to {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    public void sendPaymentFailedNotification(Subscription subscription) {
        User user = subscription.getUser();

        if(!user.isEmailNotifications()) {
            return;
        }

        log.info("Sending payment failed notification to {} for subscription: {}",
                user.getEmail(), subscription.getName());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("noreply@subscriptiontracker.com");
            message.setSubject("❌ Payment Failed - " + subscription.getName());

            String emailBody = String.format(
                    "Hi %s,\n\n" +
                            "We detected that a payment failed for your %s subscription.\n\n" +
                            "💳 Payment Details:\n" +
                            "   Service: %s (%s)\n" +
                            "   Amount: $%.2f %s\n" +
                            "   Payment Method: %s\n" +
                            "   Status: PAYMENT FAILED\n\n" +
                            "🚨 Action Required:\n" +
                            "   • Check your payment method for sufficient funds\n" +
                            "   • Update your card information if expired\n" +
                            "   • Contact your bank if the card is being declined\n\n" +
                            "⏰ Your subscription may be suspended if payment is not resolved soon.\n\n" +
                            "Update payment method: http://localhost:3000/subscriptions\n\n" +
                            "Need help? Contact support at support@subscriptiontracker.com\n\n" +
                            "Best regards,\n" +
                            "Subscription Tracker Team",

                    user.getName(),
                    subscription.getName(),
                    subscription.getName(),
                    subscription.getPlanName() != null ? subscription.getPlanName() : "Standard",
                    subscription.getPrice(),
                    subscription.getCurrency(),
                    subscription.getCard() != null ? subscription.getCard() : "Default card"
            );

            message.setText(emailBody);
            mailSender.send(message);

            log.info("✅ Successfully sent payment failed notification to {}", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send payment failed notification to {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }

    public void sendCancellationConfirmation(Subscription subscription) {
        User user = subscription.getUser();

        if (!user.isEmailNotifications()) {
            return;
        }

        log.info("Sending cancellation confirmation to {} for subscription: {}",
                user.getEmail(), subscription.getName());

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(user.getEmail());
            message.setFrom("noreply@subscriptiontracker.com");
            message.setSubject("✅ Subscription Cancelled - " + subscription.getName());

            String emailBody = String.format(
                    "Hi %s,\n\n" +
                            "Your %s subscription has been successfully cancelled.\n\n" +
                            "📋 Cancellation Details:\n" +
                            "   Service: %s (%s)\n" +
                            "   Cancelled On: %s\n" +
                            "   Service Until: %s\n" +
                            "   Final Amount: $%.2f %s\n\n" +
                            "✨ Your service will remain active until %s, so you can continue enjoying it until then.\n\n" +
                            "💰 Monthly Savings: $%.2f\n" +
                            "💰 Yearly Savings: $%.2f\n\n" +
                            "Changed your mind? You can reactivate anytime at:\n" +
                            "http://localhost:3000/subscriptions\n\n" +
                            "Thank you for using Subscription Tracker!\n\n" +
                            "Best regards,\n" +
                            "Subscription Tracker Team",

                    user.getName(),
                    subscription.getName(),
                    subscription.getName(),
                    subscription.getPlanName() != null ? subscription.getPlanName() : "Standard",
                    java.time.LocalDate.now().format(DATE_TIME_FORMATTER), // <--- FIXED: Formatter name
                    subscription.getNextRenewalDate().format(DATE_TIME_FORMATTER), // <--- FIXED: Formatter name
                    subscription.getPrice(),
                    subscription.getCurrency(),
                    subscription.getNextRenewalDate().format(DATE_TIME_FORMATTER), // <--- FIXED: Formatter name
                    subscription.getPrice(),
                    subscription.getPrice().multiply(BigDecimal.valueOf(12))
            );

            message.setText(emailBody);
            mailSender.send(message);

            log.info("✅ Successfully sent cancellation confirmation to {}", user.getEmail());

        } catch (Exception e) {
            log.error("❌ Failed to send cancellation confirmation to {}: {}",
                    user.getEmail(), e.getMessage());
        }
    }


    public void sendBulkRenewalReminders(List<Subscription> subscriptions, int daysUntilRenewal) {
        log.info("Sending {} renewal reminders for subscriptions renewing in {} days",
                subscriptions.size(), daysUntilRenewal);

        int successCount = 0;
        int failureCount = 0;

        for (Subscription subscription : subscriptions) {
            try {
                sendRenewalReminder(subscription, daysUntilRenewal);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to send reminder for subscription {}: {}",
                        subscription.getId(), e.getMessage());
                failureCount++;
            }
        }

        log.info("Bulk reminder summary - Success: {}, Failures: {}", successCount, failureCount);
    }

    public void sendTestEmail(String toEmail) {
        log.info("Sending test email to {}", toEmail);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setFrom("noreply@subscriptiontracker.com");
            message.setSubject("✅ Test Email - Subscription Tracker");
            message.setText(
                    "This is a test email to verify your email configuration is working correctly.\n\n" +
                            "If you received this email, your notification system is set up properly!\n\n" +
                            "Test Details:\n" +
                            "- SMTP Host: Configured\n" +
                            "- Email Sending: Working\n" +
                            "- Timestamp: " + java.time.LocalDateTime.now() + "\n\n" +
                            "Best regards,\n" +
                            "Subscription Tracker Team"
            );

            mailSender.send(message);
            log.info("✅ Successfully sent test email to {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send test email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Email configuration test failed: " + e.getMessage());
        }
    }
}