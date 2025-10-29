package com.example.subscriptiontracker.controller;

import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.service.NotificationService;
import com.example.subscriptiontracker.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final NotificationService notificationService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam String email) {
        try {
            notificationService.sendTestEmail(email);
            return ResponseEntity.ok("✅ Email sent! Check your inbox.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error: " + e.getMessage());
        }
    }

    @GetMapping("/debug-user-5-reminders")
    public ResponseEntity<String> debugUser5Reminders() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            StringBuilder result = new StringBuilder();
            result.append("🔍 Debug Results for User 5:\n");
            result.append("Today: ").append(today).append("\n");
            result.append("Tomorrow: ").append(tomorrow).append("\n\n");

            // Get all subscriptions for user 5
            List<Subscription> user5Subscriptions = subscriptionService.getUserSubscriptions(5L);
            result.append("User 5 has ").append(user5Subscriptions.size()).append(" total subscriptions:\n");

            for (Subscription sub : user5Subscriptions) {
                result.append("- ").append(sub.getName())
                        .append(" (renewal: ").append(sub.getNextRenewalDate())
                        .append(", status: ").append(sub.getStatus())
                        .append(", notifications: ").append(sub.isNotificationsEnabled()).append(")\n");
            }

            result.append("\n");

            // Test 1 day ahead
            List<Subscription> reminders = subscriptionService.getSubscriptionsNeedingReminders(1);
            result.append("Found ").append(reminders.size()).append(" subscriptions for tomorrow (all users)\n");

            for (Subscription sub : reminders) {
                result.append("- ").append(sub.getName())
                        .append(" (user: ").append(sub.getUser().getId())
                        .append(", renewal: ").append(sub.getNextRenewalDate()).append(")\n");
            }

            return ResponseEntity.ok(result.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("❌ Error: " + e.getMessage());
        }
    }
}