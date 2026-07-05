package com.ke.ticketsystemke.repository;

import com.ke.ticketsystemke.entity.SpeechUsageMonthly;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpeechUsageMonthlyRepository extends JpaRepository<SpeechUsageMonthly, Long> {

    Optional<SpeechUsageMonthly> findByProviderKeyAndYearMonth(String providerKey, String yearMonth);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select usage
            from SpeechUsageMonthly usage
            where usage.providerKey = :providerKey
              and usage.yearMonth = :yearMonth
            """)
    Optional<SpeechUsageMonthly> findWithLockByProviderKeyAndYearMonth(
            @Param("providerKey") String providerKey,
            @Param("yearMonth") String yearMonth
    );
}
