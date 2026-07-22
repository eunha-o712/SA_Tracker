package com.sa.trk.clan.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.clan.dto.ClanBalancedMemberDto;
import com.sa.trk.clan.dto.ClanBalancedTeamDto;
import com.sa.trk.clan.dto.ClanTeamBalanceRequest;
import com.sa.trk.clan.dto.ClanTeamBalanceResponseDto;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;

@Service
public class ClanTeamBalanceService {

    private static final int MIN_PLAYER_COUNT = 4;
    private static final int MAX_PLAYER_COUNT = 10;
    private static final int MIN_TEAM_SIZE = 2;
    private static final int MAX_TEAM_SIZE = 5;
    private static final int MAX_VARIANTS = 5;
    private static final int MAX_CACHE_ENTRIES = 500;
    private static final Duration METRICS_CACHE_DURATION = Duration.ofMinutes(10);
    private static final List<String> COMBAT_ROLES = List.of("돌격", "저격", "특수", "균형");
    private static final List<String> TEAM_KEYS = List.of("ALPHA", "BRAVO", "CHARLIE", "DELTA", "ECHO");

    private final ClanMemberRepository clanMemberRepository;
    private final MatchService matchService;
    private final WeaponService weaponService;
    private final Map<String, MetricsCacheEntry> metricsCache = new ConcurrentHashMap<>();

    public ClanTeamBalanceService(
            ClanMemberRepository clanMemberRepository,
            MatchService matchService,
            WeaponService weaponService) {
        this.clanMemberRepository = clanMemberRepository;
        this.matchService = matchService;
        this.weaponService = weaponService;
    }

    @Transactional(readOnly = true)
    public ClanTeamBalanceResponseDto balance(Long ownerId, ClanTeamBalanceRequest request) {
        if (ownerId == null || ownerId < 1) {
            throw new IllegalArgumentException("로그인 회원 정보를 확인해주세요.");
        }

        List<Long> memberIds = normalizeMemberIds(request == null ? null : request.memberIds());
        int teamSize = normalizeTeamSize(request == null ? null : request.teamSize(), memberIds.size());
        int variant = request == null || request.variant() == null
                ? 0
                : Math.max(0, request.variant());
        List<ClanMember> ownedMembers = new ArrayList<>(
                clanMemberRepository.findAllByOwnerIdAndIdIn(ownerId, memberIds)
        );
        if (ownedMembers.size() != memberIds.size()) {
            throw new IllegalArgumentException("본인 클랜 로스터에 있는 멤버만 선택할 수 있습니다.");
        }
        ownedMembers.sort(Comparator.comparing(ClanMember::getId));

        List<PlayerMetrics> metrics = ownedMembers.stream()
                .map(this::loadMetrics)
                .toList();
        double fallbackScore = metrics.stream()
                .filter(PlayerMetrics::available)
                .mapToDouble(PlayerMetrics::powerScore)
                .average()
                .orElse(50.0);
        List<PlayerMetrics> normalizedMetrics = metrics.stream()
                .map(item -> item.available() ? item : item.withPowerScore(fallbackScore))
                .toList();

        TeamCandidate candidate = selectCandidate(normalizedMetrics, teamSize, variant);
        List<ClanBalancedTeamDto> teams = new ArrayList<>();
        for (int index = 0; index < candidate.teams().size(); index++) {
            String key = TEAM_KEYS.get(index);
            teams.add(toTeam(key, key + " TEAM", candidate.teams().get(index)));
        }
        double difference = roundOne(candidate.powerDifference());
        int balanceScore = Math.max(0, (int) Math.round(100.0 - Math.min(100.0, difference)));

        return new ClanTeamBalanceResponseDto(
                normalizedMetrics.size(),
                (int) normalizedMetrics.stream().filter(PlayerMetrics::available).count(),
                teamSize,
                balanceScore,
                difference,
                candidate.variant(),
                "클랜전 승률 45% · K/D 35% · 평균 킬 20% · 주무기 역할 분산",
                List.copyOf(teams)
        );
    }

    private List<Long> normalizeMemberIds(List<Long> rawIds) {
        Set<Long> uniqueIds = new LinkedHashSet<>();
        if (rawIds != null) {
            rawIds.stream().filter(id -> id != null && id > 0).forEach(uniqueIds::add);
        }
        if (uniqueIds.size() < MIN_PLAYER_COUNT || uniqueIds.size() > MAX_PLAYER_COUNT) {
            throw new IllegalArgumentException("자동 팀 편성은 4명부터 10명까지 선택할 수 있습니다.");
        }
        return List.copyOf(uniqueIds);
    }

    private int normalizeTeamSize(Integer requestedTeamSize, int playerCount) {
        int teamSize;
        if (requestedTeamSize == null) {
            if (playerCount % 2 != 0) {
                throw new IllegalArgumentException("팀당 인원을 선택해주세요.");
            }
            teamSize = playerCount / 2;
        } else {
            teamSize = requestedTeamSize;
        }
        if (teamSize < MIN_TEAM_SIZE || teamSize > MAX_TEAM_SIZE) {
            throw new IllegalArgumentException("팀당 인원은 2명부터 5명까지 선택할 수 있습니다.");
        }
        if (playerCount < teamSize * 2) {
            throw new IllegalArgumentException("같은 인원의 팀을 2개 이상 만들 수 있도록 멤버를 더 선택해주세요.");
        }
        if (playerCount % teamSize != 0) {
            throw new IllegalArgumentException("선택 인원은 팀당 인원으로 정확히 나누어져야 합니다.");
        }
        return teamSize;
    }

    private PlayerMetrics loadMetrics(ClanMember member) {
        String cacheKey = member.getUserName().trim().toLowerCase(Locale.ROOT);
        MetricsCacheEntry cached = metricsCache.get(cacheKey);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.metrics().withIdentity(member.getId(), member.getUserName());
        }

        MatchMetrics matchMetrics = loadMatchMetrics(member.getUserName());
        WeaponMetrics weaponMetrics = loadWeaponMetrics(member.getUserName());
        double powerScore = matchMetrics.available()
                ? calculatePowerScore(matchMetrics.winRate(), matchMetrics.killDeathRatio(), matchMetrics.averageKill())
                : 0.0;
        PlayerMetrics metrics = new PlayerMetrics(
                member.getId(),
                member.getUserName(),
                weaponMetrics.primaryClass(),
                weaponMetrics.combatType(),
                roundOne(powerScore),
                matchMetrics.matchCount(),
                roundOne(matchMetrics.winRate()),
                roundOne(matchMetrics.killDeathRatio()),
                roundOne(matchMetrics.averageKill()),
                matchMetrics.available()
        );
        putCache(cacheKey, metrics);
        return metrics;
    }

    private MatchMetrics loadMatchMetrics(String userName) {
        try {
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
            if (summary == null) return MatchMetrics.unavailable();

            double averageKill = number(summary.getAverageKill());
            double averageDeath = number(summary.getAverageDeath());
            double killDeathRatio = averageDeath > 0 ? averageKill / averageDeath : averageKill;
            return new MatchMetrics(
                    value(summary.getMatchCount()),
                    number(summary.getWinRate()),
                    killDeathRatio,
                    averageKill,
                    true
            );
        } catch (RuntimeException exception) {
            return MatchMetrics.unavailable();
        }
    }

    private WeaponMetrics loadWeaponMetrics(String userName) {
        try {
            WeaponStatsResponseDto stats = weaponService.getWeaponStats(userName);
            if (stats == null) return WeaponMetrics.balanced();
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

    private TeamCandidate selectCandidate(List<PlayerMetrics> players, int teamSize, int requestedVariant) {
        List<TeamCandidate> candidates = new ArrayList<>();
        buildTeamPartitions(players, teamSize, new ArrayList<>(), candidates);
        candidates.sort(Comparator
                .comparingDouble(TeamCandidate::objective)
                .thenComparingDouble(TeamCandidate::powerDifference));
        int availableVariants = Math.min(MAX_VARIANTS, candidates.size());
        int selectedVariant = availableVariants == 0 ? 0 : requestedVariant % availableVariants;
        TeamCandidate selected = candidates.get(selectedVariant);
        return new TeamCandidate(
                selected.teams(),
                selected.objective(),
                selected.powerDifference(),
                selectedVariant
        );
    }

    private void buildTeamPartitions(
            List<PlayerMetrics> remaining,
            int teamSize,
            List<List<PlayerMetrics>> teams,
            List<TeamCandidate> candidates) {
        if (remaining.isEmpty()) {
            List<List<PlayerMetrics>> completedTeams = teams.stream().map(List::copyOf).toList();
            double powerDifference = powerDifference(completedTeams);
            double objective = powerDifference + rolePenalty(completedTeams) + unavailablePenalty(completedTeams);
            candidates.add(new TeamCandidate(completedTeams, objective, powerDifference, 0));
            return;
        }

        List<PlayerMetrics> nextTeam = new ArrayList<>();
        nextTeam.add(remaining.get(0));
        buildTeamChoices(remaining, 1, teamSize, nextTeam, teams, candidates);
    }

    private void buildTeamChoices(
            List<PlayerMetrics> remaining,
            int index,
            int teamSize,
            List<PlayerMetrics> nextTeam,
            List<List<PlayerMetrics>> teams,
            List<TeamCandidate> candidates) {
        if (nextTeam.size() == teamSize) {
            List<PlayerMetrics> nextRemaining = remaining.stream()
                    .filter(item -> !nextTeam.contains(item))
                    .toList();
            teams.add(List.copyOf(nextTeam));
            buildTeamPartitions(nextRemaining, teamSize, teams, candidates);
            teams.remove(teams.size() - 1);
            return;
        }
        if (index >= remaining.size() || nextTeam.size() + (remaining.size() - index) < teamSize) return;

        nextTeam.add(remaining.get(index));
        buildTeamChoices(remaining, index + 1, teamSize, nextTeam, teams, candidates);
        nextTeam.remove(nextTeam.size() - 1);
        buildTeamChoices(remaining, index + 1, teamSize, nextTeam, teams, candidates);
    }

    private double powerDifference(List<List<PlayerMetrics>> teams) {
        double minimum = teams.stream().mapToDouble(this::averagePower).min().orElse(0);
        double maximum = teams.stream().mapToDouble(this::averagePower).max().orElse(0);
        return maximum - minimum;
    }

    private double averagePower(List<PlayerMetrics> team) {
        return team.stream().mapToDouble(PlayerMetrics::powerScore).average().orElse(0);
    }

    private double rolePenalty(List<List<PlayerMetrics>> teams) {
        double penalty = 0;
        for (String role : COMBAT_ROLES) {
            int minimum = teams.stream().mapToInt(team -> roleCount(team, role)).min().orElse(0);
            int maximum = teams.stream().mapToInt(team -> roleCount(team, role)).max().orElse(0);
            int difference = maximum - minimum;
            penalty += difference * ("균형".equals(role) ? 1.0 : 4.0);
        }
        return penalty;
    }

    private double unavailablePenalty(List<List<PlayerMetrics>> teams) {
        long minimum = teams.stream().mapToLong(team -> team.stream().filter(item -> !item.available()).count()).min().orElse(0);
        long maximum = teams.stream().mapToLong(team -> team.stream().filter(item -> !item.available()).count()).max().orElse(0);
        return (maximum - minimum) * 2.0;
    }

    private ClanBalancedTeamDto toTeam(String key, String name, List<PlayerMetrics> metrics) {
        List<ClanBalancedMemberDto> members = metrics.stream()
                .sorted(Comparator.comparingDouble(PlayerMetrics::powerScore).reversed()
                        .thenComparing(PlayerMetrics::userName))
                .map(this::toMember)
                .toList();
        List<PlayerMetrics> available = metrics.stream().filter(PlayerMetrics::available).toList();
        return new ClanBalancedTeamDto(
                key,
                name,
                roundOne(metrics.stream().mapToDouble(PlayerMetrics::powerScore).average().orElse(0)),
                roundOne(available.stream().mapToDouble(PlayerMetrics::winRate).average().orElse(0)),
                roundOne(available.stream().mapToDouble(PlayerMetrics::killDeathRatio).average().orElse(0)),
                roundOne(available.stream().mapToDouble(PlayerMetrics::averageKill).average().orElse(0)),
                roleCounts(metrics),
                members
        );
    }

    private ClanBalancedMemberDto toMember(PlayerMetrics metrics) {
        return new ClanBalancedMemberDto(
                metrics.id(),
                metrics.userName(),
                metrics.primaryClass(),
                metrics.combatType(),
                roundOne(metrics.powerScore()),
                metrics.matchCount(),
                roundOne(metrics.winRate()),
                roundOne(metrics.killDeathRatio()),
                roundOne(metrics.averageKill()),
                metrics.available()
        );
    }

    private Map<String, Integer> roleCounts(List<PlayerMetrics> metrics) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String role : COMBAT_ROLES) {
            int count = roleCount(metrics, role);
            if (count > 0) counts.put(role, count);
        }
        return counts;
    }

    private int roleCount(List<PlayerMetrics> metrics, String role) {
        return (int) metrics.stream().filter(item -> role.equals(item.primaryClass())).count();
    }

    private String normalizeRole(String role) {
        if (COMBAT_ROLES.contains(role)) return role;
        return "균형";
    }

    private void putCache(String key, PlayerMetrics metrics) {
        Instant now = Instant.now();
        metricsCache.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        if (metricsCache.size() >= MAX_CACHE_ENTRIES) {
            metricsCache.entrySet().stream()
                    .min(Map.Entry.comparingByValue(Comparator.comparing(MetricsCacheEntry::cachedAt)))
                    .map(Map.Entry::getKey)
                    .ifPresent(metricsCache::remove);
        }
        metricsCache.put(key, new MetricsCacheEntry(metrics, now, now.plus(METRICS_CACHE_DURATION)));
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
            double winRate,
            double killDeathRatio,
            double averageKill,
            boolean available) {

        static MatchMetrics unavailable() {
            return new MatchMetrics(0, 0, 0, 0, false);
        }
    }

    private record WeaponMetrics(String primaryClass, String combatType) {
        static WeaponMetrics balanced() {
            return new WeaponMetrics("균형", "분석 대기");
        }
    }

    private record PlayerMetrics(
            Long id,
            String userName,
            String primaryClass,
            String combatType,
            double powerScore,
            int matchCount,
            double winRate,
            double killDeathRatio,
            double averageKill,
            boolean available) {

        PlayerMetrics withPowerScore(double nextPowerScore) {
            return new PlayerMetrics(
                    id, userName, primaryClass, combatType, nextPowerScore,
                    matchCount, winRate, killDeathRatio, averageKill, available
            );
        }

        PlayerMetrics withIdentity(Long nextId, String nextUserName) {
            return new PlayerMetrics(
                    nextId, nextUserName, primaryClass, combatType, powerScore,
                    matchCount, winRate, killDeathRatio, averageKill, available
            );
        }
    }

    private record MetricsCacheEntry(PlayerMetrics metrics, Instant cachedAt, Instant expiresAt) {
    }

    private record TeamCandidate(
            List<List<PlayerMetrics>> teams,
            double objective,
            double powerDifference,
            int variant) {
    }
}
