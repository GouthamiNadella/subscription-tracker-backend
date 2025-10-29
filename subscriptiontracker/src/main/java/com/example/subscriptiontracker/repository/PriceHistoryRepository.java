package com.example.subscriptiontracker.repository;

import com.example.subscriptiontracker.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findBySubscriptionIdOrderByChangeDateDesc(Long subscriptionId);

    @Query("SELECT ph FROM PriceHistory ph " +
            "WHERE ph.subscription.user.id = :userId " +
            "AND ph.changeDate >= :since " +
            "ORDER BY ph.changeDate DESC")
    List<PriceHistory> findRecentPriceChangesForUser(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT ph FROM PriceHistory ph " +
            "WHERE ph.subscription.user.id = :userId " +
            "AND ph.newPrice > ph.oldPrice " +
            "ORDER BY ph.changeDate DESC")
    List<PriceHistory> findPriceIncreasesForUser(@Param("userId") Long userId);

    @Query("SELECT ph FROM PriceHistory ph " +
            "WHERE ph.subscription.user.id = :userId " +
            "AND ph.newPrice < ph.oldPrice " +
            "ORDER BY ph.changeDate DESC")
    List<PriceHistory> findPriceDecreasesForUser(@Param("userId") Long userId);

    PriceHistory findFirstBySubscriptionIdOrderByChangeDateDesc(Long subscriptionId);

    long countBySubscriptionId(Long subscriptionId);
}
