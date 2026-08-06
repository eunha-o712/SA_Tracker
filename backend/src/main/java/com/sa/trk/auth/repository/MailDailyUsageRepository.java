package com.sa.trk.auth.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.auth.entity.MailDailyUsage;

public interface MailDailyUsageRepository extends JpaRepository<MailDailyUsage, Long> {
    Optional<MailDailyUsage> findByUsageDate(LocalDate usageDate);
}
