package com.sa.trk.clan.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.clan.dto.ClanDashboardResponseDto;
import com.sa.trk.clan.dto.ClanMemberStatsDto;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;

@Service
public class ClanDashboardService {

    private final ClanMemberRepository clanMemberRepository;
    private final ClanMemberMetricsService clanMemberMetricsService;

    public ClanDashboardService(
            ClanMemberRepository clanMemberRepository,
            ClanMemberMetricsService clanMemberMetricsService) {
        this.clanMemberRepository = clanMemberRepository;
        this.clanMemberMetricsService = clanMemberMetricsService;
    }

    @Transactional(readOnly = true)
    public ClanDashboardResponseDto getDashboard(Long ownerId, String clanName) {
        if (ownerId == null || ownerId < 1) {
            throw new IllegalArgumentException("로그인 회원 정보를 확인해주세요.");
        }
        String normalizedClanName = normalizeClanName(clanName);
        List<ClanMember> members = clanMemberRepository
                .findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(ownerId, normalizedClanName);
        List<ClanMemberStatsDto> stats = new ArrayList<>();

        for (ClanMember member : members) {
            stats.add(loadMemberStats(member));
        }

        stats.sort(Comparator
                .comparing((ClanMemberStatsDto item) -> Boolean.TRUE.equals(item.getAvailable())).reversed()
                .thenComparing(ClanMemberStatsDto::getWinRate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ClanMemberStatsDto::getAverageKillDeathRatio, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ClanMemberStatsDto::getUserName));

        List<ClanMemberStatsDto> analyzed = stats.stream()
                .filter(item -> Boolean.TRUE.equals(item.getAvailable()) && item.getMatchCount() > 0)
                .toList();

        ClanDashboardResponseDto response = new ClanDashboardResponseDto();
        response.setClanName(normalizedClanName);
        response.setMemberCount(members.size());
        response.setAnalyzedMemberCount(analyzed.size());
        response.setTotalMatchCount(analyzed.stream().mapToInt(ClanMemberStatsDto::getMatchCount).sum());
        response.setAverageWinRate(round(analyzed.stream().mapToDouble(ClanMemberStatsDto::getWinRate).average().orElse(0)));
        response.setAverageKillDeathRatio(round(analyzed.stream().mapToDouble(ClanMemberStatsDto::getAverageKillDeathRatio).average().orElse(0)));
        response.setLastRefreshedAt(members.stream()
                .map(ClanMember::getStatsUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(java.time.LocalDateTime::compareTo)
                .orElse(null));
        response.setMembers(stats);
        return response;
    }

    public ClanDashboardResponseDto refreshDashboard(Long ownerId, String clanName) {
        if (ownerId == null || ownerId < 1) {
            throw new IllegalArgumentException("로그인 회원 정보를 확인해주세요.");
        }
        String normalizedClanName = normalizeClanName(clanName);
        List<ClanMember> members = clanMemberRepository
                .findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(ownerId, normalizedClanName);
        for (ClanMember member : members) {
            clanMemberMetricsService.refreshMember(member);
        }
        return getDashboard(ownerId, normalizedClanName);
    }

    private ClanMemberStatsDto loadMemberStats(ClanMember member) {
        ClanMemberStatsDto stats = new ClanMemberStatsDto();
        stats.setId(member.getId());
        stats.setUserName(member.getUserName());
        stats.setOuid(member.getOuid());
        stats.setMatchCount(value(member.getStatsMatchCount()));
        stats.setWinCount(value(member.getStatsWinCount()));
        stats.setDrawCount(value(member.getStatsDrawCount()));
        stats.setLoseCount(value(member.getStatsLoseCount()));
        stats.setWinRate(round(number(member.getStatsWinRate())));
        stats.setAverageKillDeathRatio(round(number(member.getStatsKillDeathRatio())));
        stats.setAvailable(Boolean.TRUE.equals(member.getStatsAvailable()));
        stats.setStatsUpdatedAt(member.getStatsUpdatedAt());
        return stats;
    }

    private String normalizeClanName(String clanName) {
        if (clanName == null || clanName.isBlank()) {
            throw new IllegalArgumentException("클랜명을 확인해주세요.");
        }
        return clanName.trim();
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private double number(Double value) {
        return value == null ? 0 : value;
    }

    private double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
