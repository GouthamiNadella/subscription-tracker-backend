package com.example.subscriptiontracker.scheduler;

import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.service.NotificationService;
import com.example.subscriptiontracker.service.SubscriptionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasks {

    private final SubscriptionService subscriptionService;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 2 16 * * ?")
    @Transactional
    public void sendEmailRenewalRemainders() {
        log.info("🔔 Starting daily renewal reminder task at {}", LocalDateTime.now());

        try {

            List<Subscription> upcomingIn3Days = subscriptionService.getSubscriptionsNeedingReminders(3);
            if(!upcomingIn3Days.isEmpty()) {
                log.info("\uD83D\uDCEC Found {} subscriptions renewing in 3 days", upcomingIn3Days.size());
                notificationService.sendBulkRenewalReminders(upcomingIn3Days, 3);
            } else {
                log.info("✓ No subscriptions renewing in 3 days");
            }

            List<Subscription> upcomingIn2Days = subscriptionService.getSubscriptionsNeedingReminders(2);
            if(!upcomingIn2Days.isEmpty()) {
                log.info("📬 Found {} subscriptions renewing in 2 days", upcomingIn2Days.size());
                notificationService.sendBulkRenewalReminders(upcomingIn2Days, 2);
            } else {
                log.info("✓ No subscriptions renewing in 2 days");
            }

            List<Subscription> upcomingTomorrow = subscriptionService.getSubscriptionsNeedingReminders(1);
            if (!upcomingTomorrow.isEmpty()) {
                log.info("⚠️ Found {} subscriptions renewing TOMORROW", upcomingTomorrow.size());
                notificationService.sendBulkRenewalReminders(upcomingTomorrow, 1);
            } else {
                log.info("✓ No subscriptions renewing tomorrow");
            }

            List<Subscription> upcomingToday = subscriptionService.getSubscriptionsNeedingReminders(0);
            if (!upcomingToday.isEmpty()) {
                log.info("🚨 Found {} subscriptions renewing TODAY", upcomingToday.size());
                notificationService.sendBulkRenewalReminders(upcomingToday, 0);
            } else {
                log.info("✓ No subscriptions renewing today");
            }

            log.info("✅ Completed daily renewal reminder task successfully");
        } catch (Exception e) {
            log.error("❌ Error in daily renewal reminder task: {}", e.getMessage(), e);
        }
    }

    /*@Scheduled(cron = "0 0 10 * * SUN")
    public void sendWeeklySpendingReports() {
        log.info("\uD83D\uDCCA Starting weekly spending report task at {}", LocalDateTime.now());

        try {
            // TODO: Implement weekly report generation
            // This is optional - you can build this later
            log.info("Weekly spending reports task placeholder - implement when ready");
        } catch (Exception e) {
            log.error("❌ Error in weekly spending report task: {}", e.getMessage(), e);
        }
    }*/

    /*@Scheduled(fixedRate = 3600000) // Every 1 hour (3600000 milliseconds)
    public void monitorPaymentIssues() {
        log.debug("🔍 Running payment issue monitoring at {}", LocalDateTime.now());

        try {
            // TODO: Implement payment issue monitoring
            // This will be useful after webhook integration
            log.debug("Payment monitoring placeholder - implement with webhooks");

        } catch (Exception e) {
            log.error("❌ Error in payment issue monitoring: {}", e.getMessage(), e);
        }
    }*/
}
