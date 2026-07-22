package com.sa.trk.clan.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.clan.dto.ClanDashboardResponseDto;
import com.sa.trk.clan.dto.ClanMemberStatsDto;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;

@Service
public class ClanDashboardService {

    private static final Logger log = LoggerFactory.getLogger(ClanDashboardService.class);

    private final ClanMemberRepository clanMemberRepository;
    private final MatchService matchService;

    public ClanDashboardService(
            ClanMemberRepository clanMemberRepository,
            MatchService matchService) {
        this.clanMemberRepository = clanMemberRepository;
        this.matchService = matchService;
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
        response.setMembers(stats);
        return response;
    }

    private ClanMemberStatsDto loadMemberStats(ClanMember member) {
        ClanMemberStatsDto stats = baseStats(member);
        try {
            MatchSummaryResponseDto summary = matchService.getMatchSummary(member.getUserName());
            MatchSummaryItemDto clan = summary.getSummaries().stream()
                    .filter(item -> "CLAN".equals(item.getKey()))
                    .findFirst()
                    .orElse(null);
            if (clan == null) {
                stats.setAvailable(true);
                return stats;
            }

            stats.setMatchCount(value(clan.getMatchCount()));
            stats.setWinCount(value(clan.getWinCount()));
            stats.setDrawCount(value(clan.getDrawCount()));
            stats.setLoseCount(value(clan.getLoseCount()));
            stats.setWinRate(round(number(clan.getWinRate())));
            double death = number(clan.getAverageDeath());
            double killDeathRatio = death > 0 ? number(clan.getAverageKill()) / death : number(clan.getAverageKill());
            stats.setAverageKillDeathRatio(round(killDeathRatio));
            stats.setAvailable(true);
        } catch (RuntimeException exception) {
            log.warn("Clan member summary unavailable: userName={}", member.getUserName());
            stats.setAvailable(false);
        }
        return stats;
    }

    private ClanMemberStatsDto baseStats(ClanMember member) {
        ClanMemberStatsDto stats = new ClanMemberStatsDto();
        stats.setId(member.getId());
        stats.setUserName(member.getUserName());
        stats.setMatchCount(0);
        stats.setWinCount(0);
        stats.setDrawCount(0);
        stats.setLoseCount(0);
        stats.setWinRate(0.0);
        stats.setAverageKillDeathRatio(0.0);
        stats.setAvailable(false);
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
