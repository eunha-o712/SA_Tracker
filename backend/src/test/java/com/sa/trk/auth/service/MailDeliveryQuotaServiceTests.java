package com.sa.trk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.auth.entity.MailDailyUsage;
import com.sa.trk.auth.repository.MailDailyUsageRepository;
import com.sa.trk.config.MailDeliveryProperties;

class MailDeliveryQuotaServiceTests {

    @Mock
    private MailDailyUsageRepository repository;

    private MailDeliveryProperties properties;
    private MailDeliveryQuotaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        properties = new MailDeliveryProperties();
        properties.setEnabled(true);
        properties.setDailyLimit(500);
        properties.setZoneId("Asia/Seoul");
        service = new MailDeliveryQuotaService(repository, properties);
    }

    @Test
    void limitIsReachedAtFiveHundredSuccessfulDeliveries() {
        MailDailyUsage usage = usage(500);
        when(repository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.of(usage));

        assertThat(service.isLimitReached()).isTrue();
    }

    @Test
    void recordsSuccessfulDeliveryForKoreanCalendarDate() {
        when(repository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.empty());

        service.recordSuccessfulDelivery();

        ArgumentCaptor<MailDailyUsage> captor = ArgumentCaptor.forClass(MailDailyUsage.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsageDate()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        assertThat(captor.getValue().getSentCount()).isEqualTo(1);
    }

    @Test
    void incrementsExistingUsage() {
        MailDailyUsage usage = usage(499);
        when(repository.findByUsageDate(any(LocalDate.class))).thenReturn(Optional.of(usage));

        service.recordSuccessfulDelivery();

        assertThat(usage.getSentCount()).isEqualTo(500);
        verify(repository).save(usage);
    }

    @Test
    void disabledMailDoesNotReportLimit() {
        properties.setEnabled(false);

        assertThat(service.isLimitReached()).isFalse();
    }

    private MailDailyUsage usage(int sentCount) {
        MailDailyUsage usage = new MailDailyUsage();
        usage.setUsageDate(LocalDate.now(java.time.ZoneId.of("Asia/Seoul")));
        usage.setSentCount(sentCount);
        return usage;
    }
}
