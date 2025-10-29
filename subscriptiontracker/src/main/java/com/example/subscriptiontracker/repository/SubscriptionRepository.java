package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.model.Subscription;
import com.example.subscriptiontracker.model.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByUserId(Long userId);

    List<Subscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);

    List<Subscription> findByUserIdAndStatusNot(Long userId, SubscriptionStatus status);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    @Query("SELECT s FROM Subscription s WHERE " +
            "s.nextRenewalDate BETWEEN :start AND :end " +
            "AND s.status = 'ACTIVE' " +
            "AND s.notificationsEnabled = true")
    List<Subscription> findSubscriptionsForRenewalReminder(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Query("SELECT s FROM Subscription s WHERE " +
            "DATE(s.nextRenewalDate) = :targetDate " +
            "AND s.status = 'ACTIVE' " +
            "AND s.notificationsEnabled = true")
    List<Subscription> findByNextRenewalDate(@Param("targetDate") LocalDate targetDate);

    @Query("SELECT s FROM Subscription s WHERE " +
            "s.user.id = :userId " +
            "AND s.createdAt BETWEEN :startDate AND :endDate")
    List<Subscription> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    List<Subscription> findByUserIdAndCategory(Long UserId, String category);

    List<Subscription> findByStatus(SubscriptionStatus status);

    @Query("SELECT s FROM Subscription s WHERE " +
            "s.user.id = :userId " +
            "AND s.price > :minPrice " +
            "ORDER BY s.price DESC")
    List<Subscription> findExpensiveSubscriptions(
            @Param("userId") Long userId,
            @Param("minPrice") java.math.BigDecimal minPrice
    );



}
