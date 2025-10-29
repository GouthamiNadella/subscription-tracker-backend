package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.model.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findBySubscriptionIdOrderByEventDateDesc(Long subscriptionId);

    Optional<PaymentEvent> findByStripeEventId(String stripeEventId);

    List<PaymentEvent> findByEventTypeAndProcessedFalse(String eventType);

    @Query("SELECT pe FROM PaymentEvent pe " +
            "WHERE pe.subscription.user.id = :userId " +
            "AND pe.eventDate >= :since " +
            "ORDER BY pe.eventDate DESC")
    List<PaymentEvent> findRecentPaymentEventsForUser(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT COALESCE(SUM(pe.amount), 0) FROM PaymentEvent pe " +
            "WHERE pe.subscription.user.id = :userId " +
            "AND pe.eventType = 'PAYMENT_SUCCESS' " +
            "AND pe.eventDate BETWEEN :start AND :end")
    java.math.BigDecimal getTotalPaymentsForUserInPeriod(
            @Param("userId") Long userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    long countBySubscriptionIdAndEventType(Long subscriptionId, String eventType);

    PaymentEvent findFirstBySubscriptionIdOrderByEventDateDesc(Long subscriptionId);

    List<PaymentEvent> findByEventType(String eventType);
}
