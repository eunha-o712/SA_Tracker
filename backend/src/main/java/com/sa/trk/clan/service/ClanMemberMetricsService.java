package com.sa.trk.clan.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;

@Service
public class ClanMemberMetricsService {

    private static final Logger log = LoggerFactory.getLogger(ClanMemberMetricsService.class);
    private static final List<String> COMBAT_ROLES = List.of("돌격", "저격", "특수", "균형");

    private final ClanMemberRepository clanMemberRepository;
    private final MatchService matchService;
    private final WeaponService weaponService;

    public ClanMemberMetricsService(
            ClanMemberRepository clanMemberRepository,
            MatchService matchService,
            WeaponService weaponService) {
        this.clanMemberRepository = clanMemberRepository;
        this.matchService = matchService;
        this.weaponService = weaponService;
    }

    public ClanMember refreshMember(ClanMember member) {
        if (member == null || member.getUserName() == null || member.getUserName().isBlank()) {
            throw new IllegalArgumentException("전적을 갱신할 클랜원을 확인해주세요.");
        }

        try {
            MatchMetrics matchMetrics = loadMatchMetrics(member.getUserName());
            WeaponMetrics weaponMetrics = loadWeaponMetrics(member.getUserName());
            member.setStatsMatchCount(matchMetrics.matchCount());
            member.setStatsWinCount(matchMetrics.winCount());
            member.setStatsDrawCount(matchMetrics.drawCount());
            member.setStatsLoseCount(matchMetrics.loseCount());
            member.setStatsWinRate(roundOne(matchMetrics.winRate()));
            member.setStatsKillDeathRatio(roundOne(matchMetrics.killDeathRatio()));
            member.setStatsAverageKill(roundOne(matchMetrics.averageKill()));
            member.setStatsPrimaryClass(weaponMetrics.primaryClass());
            member.setStatsCombatType(weaponMetrics.combatType());
            member.setStatsPowerScore(roundOne(calculatePowerScore(
                    matchMetrics.winRate(),
                    matchMetrics.killDeathRatio(),
                    matchMetrics.averageKill()
            )));
            member.setStatsAvailable(true);
            member.setStatsUpdatedAt(LocalDateTime.now());
        } catch (RuntimeException exception) {
            log.warn("Clan member metrics refresh failed: userName={}", member.getUserName());
            if (member.getStatsUpdatedAt() == null) {
                member.setStatsAvailable(false);
                member.setStatsUpdatedAt(LocalDateTime.now());
            }
        }
        return clanMemberRepository.save(member);
    }

    private MatchMetrics loadMatchMetrics(String userName) {
        MatchSummaryResponseDto response = matchService.getMatchSummary(userName);
        List<MatchSummaryItemDto> summaries = response == null || response.getSummaries() == null
                ? List.of()
                : response.getSummaries();
        MatchSummaryItemDto summary = summaries.stream()
                .filter(item -> "CLAN".equals(item.getKey()) && value(item.getMatchCount()) > 0)
                .findFirst()
                .orElseGet(() -> summaries.stream()
                        .filter(item -> "RECENT".equals(item.getKey()))
                        .findFirst()
                        .orElse(null));
        if (summary == null) {
            throw new IllegalStateException("전적 요약 데이터가 없습니다.");
        }

        double averageKill = number(summary.getAverageKill());
        double averageDeath = number(summary.getAverageDeath());
        double killDeathRatio = averageDeath > 0 ? averageKill / averageDeath : averageKill;
        return new MatchMetrics(
                value(summary.getMatchCount()),
                value(summary.getWinCount()),
                value(summary.getDrawCount()),
                value(summary.getLoseCount()),
                number(summary.getWinRate()),
                killDeathRatio,
                averageKill
        );
    }

    private WeaponMetrics loadWeaponMetrics(String userName) {
        try {
            WeaponStatsResponseDto stats = weaponService.getWeaponStats(userName);
            if (stats == null) {
                return WeaponMetrics.balanced();
            }
            double totalRate = number(stats.getAssaultRate())
                    + number(stats.getSniperRate())
                    + number(stats.getSpecialRate());
            String primaryClass = totalRate <= 0 ? "균형" : normalizeRole(stats.getPrimaryClass());
            String combatType = isBlank(stats.getCombatType()) ? "분석 대기" : stats.getCombatType().trim();
            return new WeaponMetrics(primaryClass, combatType);
        } catch (RuntimeException exception) {
            return WeaponMetrics.balanced();
        }
    }

    private double calculatePowerScore(double winRate, double killDeathRatio, double averageKill) {
        double winScore = clamp(winRate, 0, 100);
        double kdScore = clamp(killDeathRatio, 0, 3) / 3.0 * 100.0;
        double killScore = clamp(averageKill, 0, 20) / 20.0 * 100.0;
        return winScore * 0.45 + kdScore * 0.35 + killScore * 0.20;
    }

    private String normalizeRole(String role) {
        return COMBAT_ROLES.contains(role) ? role : "균형";
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double number(Double value) {
        return value == null || !Double.isFinite(value) ? 0 : value;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MatchMetrics(
            int matchCount,
            int winCount,
            int drawCount,
            int loseCount,
            double winRate,
            double killDeathRatio,
            double averageKill) {
    }

    private record WeaponMetrics(String primaryClass, String combatType) {
        static WeaponMetrics balanced() {
            return new WeaponMetrics("균형", "분석 대기");
        }
    }
}
