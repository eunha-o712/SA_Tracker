package com.sa.trk.auth.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.DateTimeException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.auth.entity.MailDailyUsage;
import com.sa.trk.auth.repository.MailDailyUsageRepository;
import com.sa.trk.config.MailDeliveryProperties;

@Service
public class MailDeliveryQuotaService {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Seoul");

    private final MailDailyUsageRepository repository;
    private final MailDeliveryProperties properties;

    public MailDeliveryQuotaService(
            MailDailyUsageRepository repository,
            MailDeliveryProperties properties) {
        this.repository = repository;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public boolean isLimitReached() {
        if (!properties.isEnabled()) return false;
        return repository.findByUsageDate(today())
                .map(usage -> usage.getSentCount() >= dailyLimit())
                .orElse(false);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccessfulDelivery() {
        LocalDate today = today();
        MailDailyUsage usage = repository.findByUsageDate(today).orElseGet(() -> {
            MailDailyUsage created = new MailDailyUsage();
            created.setUsageDate(today);
            return created;
        });
        usage.setSentCount(usage.getSentCount() + 1);
        repository.save(usage);
    }

    private int dailyLimit() {
        return Math.max(1, properties.getDailyLimit());
    }

    private LocalDate today() {
        return LocalDate.now(resolveZone());
    }

    private ZoneId resolveZone() {
        try {
            return ZoneId.of(properties.getZoneId());
        } catch (DateTimeException | NullPointerException exception) {
            return DEFAULT_ZONE;
        }
    }
}
