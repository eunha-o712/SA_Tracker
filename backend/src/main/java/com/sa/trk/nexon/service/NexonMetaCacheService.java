package com.sa.trk.nexon.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.GradeDto;
import com.sa.trk.nexon.dto.LogoDto;
import com.sa.trk.nexon.dto.SeasonGradeDto;
import com.sa.trk.nexon.dto.TierDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NexonMetaCacheService {

    private final NexonApiClient nexonApiClient;

    private List<GradeDto> grades = List.of();
    private List<SeasonGradeDto> seasonGrades = List.of();
    private List<TierDto> tiers = List.of();
    private LogoDto logo;

    private LocalDateTime cachedAt;

    private boolean isExpired() {
        return cachedAt == null
                || cachedAt.isBefore(LocalDateTime.now().minusHours(24));
    }

    private synchronized void refreshIfNeeded() {
        if (!isExpired()) {
            return;
        }

        grades = nexonApiClient.getGrades();
        seasonGrades = nexonApiClient.getSeasonGrades();
        tiers = nexonApiClient.getTiers();
        logo = nexonApiClient.getLogo();

        cachedAt = LocalDateTime.now();
    }

    public String findGradeImage(String grade) {
        refreshIfNeeded();

        if (grade == null || grade.isBlank()) {
            return null;
        }

        return grades.stream()
                .filter(g -> grade.equals(g.getGrade()))
                .map(GradeDto::getGrade_image)
                .findFirst()
                .orElse(null);
    }

    public String findSeasonGradeImage(String seasonGrade) {
        refreshIfNeeded();

        if (seasonGrade == null || seasonGrade.isBlank()) {
            return null;
        }

        return seasonGrades.stream()
                .filter(g -> seasonGrade.equals(g.getSeason_grade()))
                .map(SeasonGradeDto::getSeason_grade_image)
                .findFirst()
                .orElse(null);
    }

    public String findTierImage(String tier) {
        refreshIfNeeded();

        if (tier == null || tier.isBlank()) {
            return null;
        }

        return tiers.stream()
                .filter(t -> tier.equals(t.getTier()))
                .map(TierDto::getTier_image)
                .findFirst()
                .orElse(null);
    }

    public String getLogoImage() {
        refreshIfNeeded();

        if (logo == null) {
            return null;
        }

        return logo.getLogo_image();
    }
}