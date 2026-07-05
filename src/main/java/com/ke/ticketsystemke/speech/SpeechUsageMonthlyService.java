package com.ke.ticketsystemke.speech;

import com.ke.ticketsystemke.entity.SpeechUsageMonthly;
import com.ke.ticketsystemke.repository.SpeechUsageMonthlyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.time.ZoneOffset;

@Service
public class SpeechUsageMonthlyService {

    private final SpeechUsageMonthlyRepository repository;

    public SpeechUsageMonthlyService(SpeechUsageMonthlyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SpeechUsageSnapshot getCurrentUsage(String providerKey, long monthlyLimitSeconds, boolean enabled) {
        String yearMonth = currentYearMonth();
        return repository.findByProviderKeyAndYearMonth(providerKey, yearMonth)
                .map(usage -> toSnapshot(usage, monthlyLimitSeconds, enabled))
                .orElse(new SpeechUsageSnapshot(providerKey, yearMonth, 0, monthlyLimitSeconds, enabled));
    }

    @Transactional
    public SpeechUsageSnapshot recordUsage(String providerKey, int usedSeconds, long monthlyLimitSeconds, boolean enabled) {
        String yearMonth = currentYearMonth();
        SpeechUsageMonthly usage = repository.findWithLockByProviderKeyAndYearMonth(providerKey, yearMonth)
                .orElseGet(() -> {
                    SpeechUsageMonthly created = new SpeechUsageMonthly();
                    created.setProviderKey(providerKey);
                    created.setYearMonth(yearMonth);
                    created.setUsedSeconds(0);
                    return created;
                });

        usage.setMonthlyLimitSeconds(monthlyLimitSeconds);
        usage.setEnabled(enabled);
        usage.setUsedSeconds(usage.getUsedSeconds() + Math.max(0, usedSeconds));
        return toSnapshot(repository.save(usage), monthlyLimitSeconds, enabled);
    }

    private String currentYearMonth() {
        return YearMonth.now(ZoneOffset.UTC).toString();
    }

    private SpeechUsageSnapshot toSnapshot(SpeechUsageMonthly usage, long monthlyLimitSeconds, boolean enabled) {
        return new SpeechUsageSnapshot(
                usage.getProviderKey(),
                usage.getYearMonth(),
                usage.getUsedSeconds(),
                monthlyLimitSeconds,
                enabled
        );
    }
}
