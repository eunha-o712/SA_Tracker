package com.sa.trk.match.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.sa.trk.match.dto.MatchDetailResponseDto;
import com.sa.trk.match.dto.HeadshotStatsDto;
import com.sa.trk.match.dto.MatchListResponseDto;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.dto.MatchTrendPointDto;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.MatchDetailDto;
import com.sa.trk.nexon.dto.MatchDetailItemDto;
import com.sa.trk.nexon.dto.MatchDto;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;

@Service
public class MatchService {

    private static final Logger log = Logger.getLogger(MatchService.class.getName());

    private static final int PAGE_SIZE = 20;
    private static final int SUMMARY_SIZE = 20;
    private static final int HEADSHOT_SAMPLE_SIZE = 20;
    private static final int MAP_SEARCH_LIMIT = 100;
    private static final int MAX_OUID_CACHE_ENTRIES = 500;
    private static final int MAX_LIST_CACHE_ENTRIES = 100;
    private static final int MAX_DETAIL_CACHE_ENTRIES = 1_000;
    private static final int MAX_CLAN_CACHE_ENTRIES = 1_000;
    private static final Duration LIST_CACHE_DURATION = Duration.ofMinutes(10);
    private static final Duration DETAIL_CACHE_DURATION = Duration.ofMinutes(30);
    private static final Duration CLAN_CACHE_DURATION = Duration.ofMinutes(30);
    private static final Duration OUID_CACHE_DURATION = Duration.ofMinutes(30);

    private static final List<SummaryQuery> GENERAL_SUMMARY_QUERIES = List.of(
            new SummaryQuery("폭파미션", "일반전"),
            new SummaryQuery("데스매치", "일반전"),
            new SummaryQuery("개인전", "일반전")
    );

    private static final List<SummaryQuery> CLAN_SUMMARY_QUERIES = List.of(
            new SummaryQuery("폭파미션", "클랜전"),
            new SummaryQuery("데스매치", "클랜전"),
            new SummaryQuery("폭파미션", "퀵매치 클랜전"),
            new SummaryQuery("데스매치", "퀵매치 클랜전")
    );

    private static final List<SummaryQuery> RANKED_SUMMARY_QUERIES = List.of(
            new SummaryQuery("폭파미션", "랭크전 솔로"),
            new SummaryQuery("폭파미션", "랭크전 파티"),
            new SummaryQuery("폭파미션", "클랜 랭크전")
    );

    private static final List<SummaryQuery> FAVORITE_SUMMARY_QUERIES = List.of(
            new SummaryQuery("폭파미션", "일반전"),
            new SummaryQuery("데스매치", "일반전"),
            new SummaryQuery("개인전", "일반전"),
            new SummaryQuery("폭파미션", "클랜전"),
            new SummaryQuery("데스매치", "클랜전"),
            new SummaryQuery("폭파미션", "퀵매치 클랜전"),
            new SummaryQuery("데스매치", "퀵매치 클랜전"),
            new SummaryQuery("폭파미션", "랭크전 솔로"),
            new SummaryQuery("폭파미션", "랭크전 파티"),
            new SummaryQuery("폭파미션", "클랜 랭크전")
    );

    private final NexonApiClient nexonApiClient;
    private final Map<MatchCacheKey, MatchCacheEntry> matchCache = new ConcurrentHashMap<>();
    private final Map<String, DetailCacheEntry> detailCache = new ConcurrentHashMap<>();
    private final Map<String, ClanCacheEntry> clanCache = new ConcurrentHashMap<>();
    private final Map<String, OuidCacheEntry> ouidCache = new ConcurrentHashMap<>();

    public MatchService(NexonApiClient nexonApiClient) {
        this.nexonApiClient = nexonApiClient;
    }

    public MatchListResponseDto getMatches(
            String userName,
            Integer page,
            String scope,
            String matchType,
            String matchMode,
            String matchMap) {

        String normalizedUserName = requireValue(userName, "닉네임");
        String normalizedScope = requireValue(scope, "조회 범위").toUpperCase(Locale.ROOT);
        String normalizedMatchType = requireValue(matchType, "매치 유형");
        String normalizedMatchMode = isBlank(matchMode) ? "ALL" : matchMode.trim();
        String normalizedMatchMap = isBlank(matchMap) ? "ALL" : matchMap.trim();

        List<String> matchModes = isAll(normalizedMatchMode)
                ? resolveMatchModes(normalizedScope)
                : List.of(normalizedMatchMode);
        Map<String, MatchDto> uniqueMatches = new LinkedHashMap<>();

        for (String queriedMode : matchModes) {
            List<MatchDto> modeMatches = loadMatches(
                    normalizedUserName,
                    queriedMode,
                    normalizedMatchType
            );

            for (MatchDto match : modeMatches) {
                if (match != null && !isBlank(match.getMatch_id())) {
                    uniqueMatches.putIfAbsent(match.getMatch_id(), match);
                }
            }
        }

        List<MatchDto> filteredMatches = new ArrayList<>(uniqueMatches.values());
        filteredMatches.sort(matchDateComparator());

        if (!isAll(normalizedMatchMap)) {
            filteredMatches = filterByMap(filteredMatches, normalizedMatchMap);
        }

        int currentPage = page == null || page < 1 ? 1 : page;
        int totalCount = filteredMatches.size();
        int fromIndex = (currentPage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalCount);

        List<MatchDto> pagedMatches = fromIndex >= totalCount
                ? List.of()
                : new ArrayList<>(filteredMatches.subList(fromIndex, toIndex));

        MatchListResponseDto response = new MatchListResponseDto();
        response.setUserName(normalizedUserName);
        response.setPage(currentPage);
        response.setSize(PAGE_SIZE);
        response.setTotalCount(totalCount);
        response.setMatches(pagedMatches);
        return response;
    }

    public MatchListResponseDto getMatchesByOuid(
            String userName,
            String ouid,
            Integer page,
            String scope,
            String matchType,
            String matchMode,
            String matchMap) {
        rememberOuid(userName, ouid);
        return getMatches(userName, page, scope, matchType, matchMode, matchMap);
    }

    public MatchSummaryResponseDto getMatchSummary(String userName) {
        String normalizedUserName = requireValue(userName, "닉네임");
        return getMatchSummary(normalizedUserName, resolveOuid(normalizedUserName));
    }

    private MatchSummaryResponseDto getMatchSummary(String normalizedUserName, String ouid) {

        List<MatchDto> generalMatches = loadSummaryMatches(
                normalizedUserName,
                ouid,
                GENERAL_SUMMARY_QUERIES
        );
        List<MatchDto> clanMatches = loadSummaryMatches(
                normalizedUserName,
                ouid,
                CLAN_SUMMARY_QUERIES
        );
        List<MatchDto> rankedMatches = loadSummaryMatches(
                normalizedUserName,
                ouid,
                RANKED_SUMMARY_QUERIES
        );

        List<MatchDto> recentMatches = mergeMatches(
                generalMatches,
                clanMatches,
                rankedMatches
        );

        MatchSummaryResponseDto response = new MatchSummaryResponseDto();
        response.setUserName(normalizedUserName);
        List<MatchDto> recentSample = sampleMatches(recentMatches);
        response.setPrimaryMode(findPrimaryValue(recentSample, true));
        response.setPrimaryType(findPrimaryValue(recentSample, false));
        response.setPlayStyle(determinePlayStyle(recentSample));
        response.setKillDeathTrend(createKillDeathTrend(recentSample));
        response.setSummaries(List.of(
                createSummary("RECENT", "최근 매치", recentMatches),
                createSummary("CLAN", "클랜전", clanMatches),
                createSummary("RANKED", "랭크전", rankedMatches),
                createSummary("GENERAL", "일반전", generalMatches)
        ));
        return response;
    }

    public MatchSummaryResponseDto getMatchSummaryByOuid(String userName, String ouid) {
        rememberOuid(userName, ouid);
        return getMatchSummary(requireValue(userName, "닉네임"), requireValue(ouid, "OUID"));
    }

    public FavoriteMatchSummaryRefresh getFavoriteMatchSummaryByOuid(
            String userName,
            String ouid,
            List<Integer> preferredQueryIndexes,
            boolean discoverAllQueries) {
        String normalizedUserName = requireValue(userName, "닉네임");
        rememberOuid(normalizedUserName, ouid);

        List<Integer> queryIndexes = discoverAllQueries
                ? allFavoriteQueryIndexes()
                : normalizeFavoriteQueryIndexes(preferredQueryIndexes);
        if (queryIndexes.isEmpty()) {
            queryIndexes = allFavoriteQueryIndexes();
            discoverAllQueries = true;
        }

        Map<String, MatchDto> uniqueMatches = new LinkedHashMap<>();
        List<Integer> activeQueryIndexes = new ArrayList<>();
        for (Integer queryIndex : queryIndexes) {
            SummaryQuery query = FAVORITE_SUMMARY_QUERIES.get(queryIndex);
            try {
                List<MatchDto> matches = loadMatchesByOuid(
                        normalizedUserName,
                        ouid,
                        query.matchMode(),
                        query.matchType()
                );
                if (!matches.isEmpty()) {
                    activeQueryIndexes.add(queryIndex);
                }
                for (MatchDto match : matches) {
                    if (match != null && !isBlank(match.getMatch_id())) {
                        uniqueMatches.putIfAbsent(match.getMatch_id(), match);
                    }
                }
            } catch (HttpClientErrorException.TooManyRequests exception) {
                throw exception;
            } catch (RuntimeException exception) {
                log.warning("즐겨찾기 전적 범위 조회 실패 ["
                        + query.matchMode() + "/" + query.matchType() + "]: "
                        + exception.getMessage());
            }
        }

        List<MatchDto> recentMatches = new ArrayList<>(uniqueMatches.values());
        recentMatches.sort(matchDateComparator());
        MatchSummaryResponseDto response = new MatchSummaryResponseDto();
        response.setUserName(normalizedUserName);
        response.setSummaries(List.of(
                createSummary("RECENT", "최근 매치", recentMatches)
        ));
        return new FavoriteMatchSummaryRefresh(
                response,
                List.copyOf(activeQueryIndexes),
                discoverAllQueries
        );
    }

    public List<Integer> resolveFavoriteQueryIndexes(
            String scope,
            String matchMode,
            String matchType) {
        String normalizedMatchType = requireValue(matchType, "매치 유형");
        String normalizedMatchMode = isBlank(matchMode) ? "ALL" : matchMode.trim();
        List<String> queriedModes = isAll(normalizedMatchMode)
                ? resolveMatchModes(requireValue(scope, "조회 범위").toUpperCase(Locale.ROOT))
                : List.of(normalizedMatchMode);

        return IntStream.range(0, FAVORITE_SUMMARY_QUERIES.size())
                .filter(index -> {
                    SummaryQuery query = FAVORITE_SUMMARY_QUERIES.get(index);
                    return queriedModes.contains(query.matchMode())
                            && normalizedMatchType.equals(query.matchType());
                })
                .boxed()
                .toList();
    }

    public List<MatchDto> getRecentMatchesWithMap(String userName) {
        return getRecentMatchesWithMap(userName, SUMMARY_SIZE);
    }

    public List<MatchDto> getRecentMatchesWithMap(String userName, int sampleSize) {
        List<MatchDto> recentMatches = getRecentMatchSample(userName, sampleSize);

        for (MatchDto match : recentMatches) {
            if (!isBlank(match.getMatch_map())) {
                continue;
            }
            MatchDetailDto detail = getDetailSafely(match.getMatch_id());
            if (detail != null && !isBlank(detail.getMatch_map())) {
                match.setMatch_map(detail.getMatch_map().trim());
            }
        }
        return recentMatches;
    }

    public String getRecentMatchVersion(String userName, int sampleSize) {
        List<MatchDto> recentMatches = getRecentMatchSample(userName, sampleSize);
        StringBuilder snapshot = new StringBuilder();
        for (MatchDto match : recentMatches) {
            snapshot.append(textValue(match.getMatch_id())).append('|')
                    .append(textValue(match.getDate_match())).append('|')
                    .append(textValue(match.getMatch_result())).append('|')
                    .append(numberOrZero(match.getKill())).append('|')
                    .append(numberOrZero(match.getDeath())).append('|')
                    .append(numberOrZero(match.getAssist())).append(';');
        }
        return sha256(snapshot.toString());
    }

    private List<MatchDto> getRecentMatchSample(String userName, int sampleSize) {
        String normalizedUserName = requireValue(userName, "닉네임");
        int normalizedSampleSize = Math.max(1, Math.min(sampleSize, SUMMARY_SIZE));
        List<MatchDto> mergedMatches = mergeMatches(
                loadSummaryMatches(normalizedUserName, GENERAL_SUMMARY_QUERIES),
                loadSummaryMatches(normalizedUserName, CLAN_SUMMARY_QUERIES),
                loadSummaryMatches(normalizedUserName, RANKED_SUMMARY_QUERIES)
        );
        return mergedMatches.size() <= normalizedSampleSize
                ? new ArrayList<>(mergedMatches)
                : new ArrayList<>(mergedMatches.subList(0, normalizedSampleSize));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("매치 버전을 생성할 수 없습니다.", exception);
        }
    }

    private String textValue(String value) {
        return value == null ? "" : value.trim();
    }

    public HeadshotStatsDto getHeadshotStats(String userName) {
        String normalizedUserName = requireValue(userName, "닉네임");
        return getHeadshotStats(normalizedUserName, resolveOuid(normalizedUserName));
    }

    private HeadshotStatsDto getHeadshotStats(String normalizedUserName, String ouid) {
        List<MatchDto> recentMatches = sampleMatches(mergeMatches(
                loadSummaryMatches(normalizedUserName, ouid, GENERAL_SUMMARY_QUERIES),
                loadSummaryMatches(normalizedUserName, ouid, CLAN_SUMMARY_QUERIES),
                loadSummaryMatches(normalizedUserName, ouid, RANKED_SUMMARY_QUERIES)
        ));

        int sampleMatchCount = 0;
        int totalKills = 0;
        int totalHeadshots = 0;

        for (MatchDto match : recentMatches.stream().limit(HEADSHOT_SAMPLE_SIZE).toList()) {
            MatchDetailDto detail = getDetailSafely(match.getMatch_id());
            if (detail == null || detail.getMatch_detail() == null) {
                continue;
            }

            MatchDetailItemDto player = findTargetPlayer(
                    match,
                    detail.getMatch_detail(),
                    normalizedUserName,
                    ouid
            );
            if (player == null) {
                continue;
            }

            sampleMatchCount++;
            totalKills += numberOrZero(player.getKill());
            totalHeadshots += numberOrZero(player.getHeadshot());
        }

        HeadshotStatsDto stats = new HeadshotStatsDto();
        stats.setSampleMatchCount(sampleMatchCount);
        stats.setTotalKills(totalKills);
        stats.setTotalHeadshots(totalHeadshots);
        stats.setHeadshotRate(totalKills == 0 ? 0.0 : roundOne(totalHeadshots * 100.0 / totalKills));
        stats.setAverageHeadshots(sampleMatchCount == 0 ? 0.0 : roundOne(totalHeadshots / (double) sampleMatchCount));
        return stats;
    }

    public HeadshotStatsDto getHeadshotStatsByOuid(String userName, String ouid) {
        String normalizedUserName = requireValue(userName, "닉네임");
        String normalizedOuid = requireValue(ouid, "OUID");
        rememberOuid(normalizedUserName, normalizedOuid);
        return getHeadshotStats(normalizedUserName, normalizedOuid);
    }

    private MatchDetailItemDto findTargetPlayer(
            MatchDto match,
            List<MatchDetailItemDto> players,
            String userName,
            String ouid) {
        if (players == null || players.isEmpty()) {
            return null;
        }

        MatchDetailItemDto ouidMatch = players.stream()
                .filter(player -> sameText(player.getOuid(), ouid))
                .findFirst()
                .orElse(null);
        if (ouidMatch != null) {
            return ouidMatch;
        }

        MatchDetailItemDto nameMatch = players.stream()
                .filter(player -> sameText(player.getUser_name(), userName))
                .findFirst()
                .orElse(null);
        if (nameMatch != null) {
            return nameMatch;
        }

        List<MatchDetailItemDto> statMatches = players.stream()
                .filter(player -> sameMatchRecord(match, player))
                .toList();
        return statMatches.size() == 1 ? statMatches.get(0) : null;
    }

    private boolean sameMatchRecord(MatchDto match, MatchDetailItemDto player) {
        return numberOrZero(match.getKill()) == numberOrZero(player.getKill())
                && numberOrZero(match.getDeath()) == numberOrZero(player.getDeath())
                && numberOrZero(match.getAssist()) == numberOrZero(player.getAssist())
                && sameText(match.getMatch_result(), player.getMatch_result());
    }

    private synchronized void rememberOuid(String userName, String ouid) {
        String normalizedUserName = requireValue(userName, "nickname");
        String normalizedOuid = requireValue(ouid, "OUID");
        String cacheKey = normalizedUserName.toLowerCase(Locale.ROOT);
        ouidCache.put(
                cacheKey,
                new OuidCacheEntry(
                        normalizedOuid,
                        Instant.now(),
                        Instant.now().plus(OUID_CACHE_DURATION)
                )
        );
    }

    private List<MatchDto> sampleMatches(List<MatchDto> matches) {
        return matches.size() <= SUMMARY_SIZE
                ? new ArrayList<>(matches)
                : new ArrayList<>(matches.subList(0, SUMMARY_SIZE));
    }

    private List<Integer> allFavoriteQueryIndexes() {
        return IntStream.range(0, FAVORITE_SUMMARY_QUERIES.size())
                .boxed()
                .toList();
    }

    private List<Integer> normalizeFavoriteQueryIndexes(List<Integer> queryIndexes) {
        if (queryIndexes == null) {
            return List.of();
        }
        return queryIndexes.stream()
                .filter(index -> index != null
                        && index >= 0
                        && index < FAVORITE_SUMMARY_QUERIES.size())
                .distinct()
                .sorted()
                .toList();
    }

    private String findPrimaryValue(List<MatchDto> matches, boolean mode) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MatchDto match : matches) {
            String value = mode ? match.getMatch_mode() : match.getMatch_type();
            if (!isBlank(value)) {
                counts.merge(value.trim(), 1, Integer::sum);
            }
        }

        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("-");
    }

    private String determinePlayStyle(List<MatchDto> matches) {
        if (matches.isEmpty()) {
            return "분석 대기";
        }

        double averageKill = matches.stream()
                .mapToInt(match -> numberOrZero(match.getKill()))
                .average()
                .orElse(0.0);
        double averageDeath = matches.stream()
                .mapToInt(match -> numberOrZero(match.getDeath()))
                .average()
                .orElse(0.0);
        double averageAssist = matches.stream()
                .mapToInt(match -> numberOrZero(match.getAssist()))
                .average()
                .orElse(0.0);
        double killDeathRatio = averageKill / Math.max(averageDeath, 1.0);

        if (averageAssist >= 3.0 && averageAssist >= averageKill * 0.35) {
            return "지원형";
        }
        if (averageKill >= 8.0 && averageKill >= averageDeath * 0.9) {
            return "공격형";
        }
        if (averageDeath <= 7.0 || killDeathRatio >= 1.15) {
            return "안정형";
        }
        return "균형형";
    }

    private List<MatchTrendPointDto> createKillDeathTrend(List<MatchDto> matches) {
        List<MatchTrendPointDto> trend = new ArrayList<>();
        for (int index = matches.size() - 1; index >= 0; index--) {
            MatchDto match = matches.get(index);
            int kill = numberOrZero(match.getKill());
            int death = numberOrZero(match.getDeath());

            MatchTrendPointDto point = new MatchTrendPointDto();
            point.setDateMatch(match.getDate_match());
            point.setKill(kill);
            point.setDeath(death);
            point.setKillDeathRatio(
                    Math.round((kill / (double) Math.max(death, 1)) * 100.0) / 100.0
            );
            point.setResult(convertMatchResult(match.getMatch_result()));
            trend.add(point);
        }
        return trend;
    }

    private List<MatchDto> loadSummaryMatches(
            String userName,
            List<SummaryQuery> queries) {
        return loadSummaryMatches(userName, resolveOuid(userName), queries);
    }

    private List<MatchDto> loadSummaryMatches(
            String userName,
            String ouid,
            List<SummaryQuery> queries) {

        Map<String, MatchDto> uniqueMatches = new LinkedHashMap<>();

        for (SummaryQuery query : queries) {
            try {
                List<MatchDto> matches = loadMatchesByOuid(
                        userName,
                        ouid,
                        query.matchMode(),
                        query.matchType()
                );

                for (MatchDto match : matches) {
                    if (match != null && !isBlank(match.getMatch_id())) {
                        uniqueMatches.putIfAbsent(match.getMatch_id(), match);
                    }
                }
            } catch (HttpClientErrorException.TooManyRequests exception) {
                throw exception;
            } catch (Exception exception) {
                log.warning("매치 요약 쿼리 실패 [" + query.matchMode() + "/" + query.matchType() + "]: " + exception.getMessage());
            }
        }

        List<MatchDto> mergedMatches = new ArrayList<>(uniqueMatches.values());
        mergedMatches.sort(matchDateComparator());
        return mergedMatches;
    }

    @SafeVarargs
    private final List<MatchDto> mergeMatches(List<MatchDto>... matchGroups) {
        Map<String, MatchDto> uniqueMatches = new LinkedHashMap<>();

        for (List<MatchDto> matches : matchGroups) {
            for (MatchDto match : matches) {
                if (match != null && !isBlank(match.getMatch_id())) {
                    uniqueMatches.putIfAbsent(match.getMatch_id(), match);
                }
            }
        }

        List<MatchDto> mergedMatches = new ArrayList<>(uniqueMatches.values());
        mergedMatches.sort(matchDateComparator());
        return mergedMatches;
    }

    private MatchSummaryItemDto createSummary(
            String key,
            String label,
            List<MatchDto> matches) {

        List<MatchDto> sampleMatches = sampleMatches(matches);

        long totalKill = 0;
        long totalDeath = 0;
        long totalAssist = 0;
        int winCount = 0;
        int drawCount = 0;
        int loseCount = 0;
        List<String> recentResults = new ArrayList<>();

        for (MatchDto match : sampleMatches) {
            totalKill += numberOrZero(match.getKill());
            totalDeath += numberOrZero(match.getDeath());
            totalAssist += numberOrZero(match.getAssist());

            String result = convertMatchResult(match.getMatch_result());
            recentResults.add(result);

            switch (result) {
                case "W" -> winCount++;
                case "D" -> drawCount++;
                case "L" -> loseCount++;
                default -> {
                }
            }
        }

        int matchCount = sampleMatches.size();
        MatchSummaryItemDto summary = new MatchSummaryItemDto();
        summary.setKey(key);
        summary.setLabel(label);
        summary.setLatestMatchDate(
                sampleMatches.isEmpty() ? null : sampleMatches.get(0).getDate_match()
        );
        summary.setMatchCount(matchCount);
        summary.setAverageKill(average(totalKill, matchCount));
        summary.setAverageDeath(average(totalDeath, matchCount));
        summary.setAverageAssist(average(totalAssist, matchCount));
        summary.setWinCount(winCount);
        summary.setDrawCount(drawCount);
        summary.setLoseCount(loseCount);
        summary.setWinRate(calculateWinRate(winCount, drawCount, loseCount));
        summary.setRecentResults(recentResults);
        return summary;
    }

    private String convertMatchResult(String matchResult) {
        if ("1".equals(matchResult)) {
            return "W";
        }
        if ("2".equals(matchResult)) {
            return "L";
        }
        if ("3".equals(matchResult)) {
            return "D";
        }
        return "";
    }

    private double calculateWinRate(int wins, int draws, int losses) {
        int resultCount = wins + draws + losses;
        if (resultCount == 0) {
            return 0.0;
        }
        return Math.round((wins * 1_000.0 / resultCount)) / 10.0;
    }

    private int numberOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double average(long total, int count) {
        if (count == 0) {
            return 0.0;
        }
        return Math.round((total * 10.0 / count)) / 10.0;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<String> resolveMatchModes(String scope) {
        return switch (scope) {
            case "RECENT" -> List.of("폭파미션", "데스매치", "개인전");
            case "BOMB" -> List.of("폭파미션");
            case "DEATHMATCH" -> List.of("데스매치");
            case "SOLO" -> List.of("개인전");
            case "CLAN" -> List.of("폭파미션");
            default -> throw new IllegalArgumentException("지원하지 않는 조회 범위입니다: " + scope);
        };
    }

    private List<MatchDto> loadMatches(
            String userName,
            String matchMode,
            String matchType) {
        return loadMatchesByOuid(userName, resolveOuid(userName), matchMode, matchType);
    }

    private List<MatchDto> loadMatchesByOuid(
            String userName,
            String ouid,
            String matchMode,
            String matchType) {

        removeExpiredListCacheEntries();

        MatchCacheKey cacheKey = new MatchCacheKey(
                userName.toLowerCase(Locale.ROOT),
                matchMode,
                matchType
        );

        MatchCacheEntry cachedEntry = matchCache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return new ArrayList<>(cachedEntry.matches());
        }

        List<MatchDto> matches = new ArrayList<>(requestMatches(
                ouid,
                matchMode,
                matchType
        ));
        matches.sort(matchDateComparator());

        if (matchCache.size() >= MAX_LIST_CACHE_ENTRIES) {
            removeOldestListCacheEntry();
        }

        MatchCacheEntry newEntry = new MatchCacheEntry(
                List.copyOf(matches),
                Instant.now(),
                Instant.now().plus(LIST_CACHE_DURATION)
        );
        matchCache.put(cacheKey, newEntry);
        return new ArrayList<>(newEntry.matches());
    }

    private List<MatchDto> requestMatches(
            String ouid,
            String matchMode,
            String matchType) {
        List<MatchDto> matches = nexonApiClient.getMatches(ouid, matchMode, matchType);
        return matches == null ? List.of() : matches;
    }

    private synchronized String resolveOuid(String userName) {
        removeExpiredOuidCacheEntries();

        String cacheKey = userName.trim().toLowerCase(Locale.ROOT);
        OuidCacheEntry cachedEntry = ouidCache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.ouid();
        }

        OuidResponseDto ouidResponse = nexonApiClient.getOuid(userName);
        if (ouidResponse == null || isBlank(ouidResponse.getOuid())) {
            throw new IllegalArgumentException("사용자 계정 식별자를 찾을 수 없습니다.");
        }

        if (ouidCache.size() >= MAX_OUID_CACHE_ENTRIES) {
            removeOldestOuidCacheEntry();
        }

        String ouid = ouidResponse.getOuid().trim();
        ouidCache.put(
                cacheKey,
                new OuidCacheEntry(
                        ouid,
                        Instant.now(),
                        Instant.now().plus(OUID_CACHE_DURATION)
                )
        );
        return ouid;
    }

    private List<MatchDto> filterByMap(List<MatchDto> matches, String targetMap) {
        List<MatchDto> filtered = new ArrayList<>();
        int scanSize = Math.min(matches.size(), MAP_SEARCH_LIMIT);

        for (int index = 0; index < scanSize; index++) {
            MatchDto match = matches.get(index);
            MatchDetailDto detail = getDetailSafely(match.getMatch_id());

            if (detail != null && sameText(detail.getMatch_map(), targetMap)) {
                match.setMatch_map(detail.getMatch_map());
                filtered.add(match);
            }
        }

        return filtered;
    }

    public MatchDetailResponseDto getMatchDetail(String matchId) {
        String normalizedMatchId = requireValue(matchId, "매치 ID");
        MatchDetailDto detail = getDetail(normalizedMatchId);

        MatchDetailResponseDto response = new MatchDetailResponseDto();
        if (detail == null) {
            response.setMatchId(normalizedMatchId);
            response.setMatchDetail(List.of());
            return response;
        }

        fillMissingClanNames(detail);

        response.setMatchId(detail.getMatch_id());
        response.setMatchType(detail.getMatch_type());
        response.setMatchMode(detail.getMatch_mode());
        response.setDateMatch(detail.getDate_match());
        response.setMatchMap(detail.getMatch_map());
        response.setMatchDetail(
                detail.getMatch_detail() == null ? List.of() : detail.getMatch_detail()
        );
        return response;
    }

    private void fillMissingClanNames(MatchDetailDto detail) {
        if (!isClanMatchType(detail.getMatch_type()) || detail.getMatch_detail() == null) {
            return;
        }

        Map<String, List<MatchDetailItemDto>> teams = new LinkedHashMap<>();
        for (MatchDetailItemDto player : detail.getMatch_detail()) {
            if (player == null) {
                continue;
            }
            String teamId = isBlank(player.getTeam_id()) ? "UNKNOWN" : player.getTeam_id().trim();
            teams.computeIfAbsent(teamId, ignored -> new ArrayList<>()).add(player);
        }

        for (List<MatchDetailItemDto> teamPlayers : teams.values()) {
            String teamClanName = findExistingClanName(teamPlayers);

            if (isBlank(teamClanName)) {
                teamClanName = findClanNameFromPlayerBasic(teamPlayers);
            }

            if (isBlank(teamClanName)) {
                continue;
            }

            for (MatchDetailItemDto player : teamPlayers) {
                if (isBlank(player.getClan_name())) {
                    player.setClan_name(teamClanName);
                }
            }
        }
    }

    private String findExistingClanName(List<MatchDetailItemDto> players) {
        return players.stream()
                .map(MatchDetailItemDto::getClan_name)
                .filter(clanName -> !isBlank(clanName))
                .findFirst()
                .orElse(null);
    }

    private String findClanNameFromPlayerBasic(List<MatchDetailItemDto> players) {
        for (MatchDetailItemDto player : players) {
            if (isBlank(player.getUser_name())) {
                continue;
            }

            String clanName = getClanName(player.getUser_name());
            if (!isBlank(clanName)) {
                return clanName;
            }
        }
        return null;
    }

    private synchronized String getClanName(String userName) {
        removeExpiredClanCacheEntries();

        String cacheKey = userName.trim().toLowerCase(Locale.ROOT);
        ClanCacheEntry cachedEntry = clanCache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.clanName();
        }

        String clanName = requestClanName(userName);

        if (clanCache.size() >= MAX_CLAN_CACHE_ENTRIES) {
            removeOldestClanCacheEntry();
        }

        clanCache.put(
                cacheKey,
                new ClanCacheEntry(
                        clanName,
                        Instant.now(),
                        Instant.now().plus(CLAN_CACHE_DURATION)
                )
        );
        return clanName;
    }

    private String requestClanName(String userName) {
        try {
            OuidResponseDto ouidResponse = nexonApiClient.getOuid(userName);
            if (ouidResponse == null || isBlank(ouidResponse.getOuid())) {
                return "";
            }

            UserBasicDto basic = nexonApiClient.getUserBasic(ouidResponse.getOuid());
            return basic == null || isBlank(basic.getClan_name())
                    ? ""
                    : basic.getClan_name().trim();
        } catch (RuntimeException exception) {
            return "";
        }
    }

    private boolean isClanMatchType(String matchType) {
        String normalizedType = normalizeText(matchType);
        return normalizedType.equals(normalizeText("클랜전"))
                || normalizedType.equals(normalizeText("퀵매치 클랜전"))
                || normalizedType.equals(normalizeText("클랜 랭크전"));
    }

    private MatchDetailDto getDetailSafely(String matchId) {
        try {
            return getDetail(matchId);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private synchronized MatchDetailDto getDetail(String matchId) {
        removeExpiredDetailCacheEntries();

        DetailCacheEntry cachedEntry = detailCache.get(matchId);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.detail();
        }

        MatchDetailDto detail = requestDetail(matchId);
        if (detail == null) {
            return null;
        }

        if (detailCache.size() >= MAX_DETAIL_CACHE_ENTRIES) {
            removeOldestDetailCacheEntry();
        }

        detailCache.put(
                matchId,
                new DetailCacheEntry(
                        detail,
                        Instant.now(),
                        Instant.now().plus(DETAIL_CACHE_DURATION)
                )
        );
        return detail;
    }

    private MatchDetailDto requestDetail(String matchId) {
        return nexonApiClient.getMatchDetail(matchId);
    }

    private Comparator<MatchDto> matchDateComparator() {
        return Comparator.comparing(
                MatchDto::getDate_match,
                Comparator.nullsLast(Comparator.reverseOrder())
        );
    }

    private void removeExpiredListCacheEntries() {
        matchCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void removeExpiredDetailCacheEntries() {
        detailCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void removeExpiredClanCacheEntries() {
        clanCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void removeExpiredOuidCacheEntries() {
        ouidCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private void removeOldestListCacheEntry() {
        matchCache.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        Comparator.comparing(MatchCacheEntry::cachedAt)
                ))
                .map(Map.Entry::getKey)
                .ifPresent(matchCache::remove);
    }

    private void removeOldestDetailCacheEntry() {
        detailCache.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        Comparator.comparing(DetailCacheEntry::cachedAt)
                ))
                .map(Map.Entry::getKey)
                .ifPresent(detailCache::remove);
    }

    private void removeOldestClanCacheEntry() {
        clanCache.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        Comparator.comparing(ClanCacheEntry::cachedAt)
                ))
                .map(Map.Entry::getKey)
                .ifPresent(clanCache::remove);
    }

    private void removeOldestOuidCacheEntry() {
        ouidCache.entrySet().stream()
                .min(Map.Entry.comparingByValue(
                        Comparator.comparing(OuidCacheEntry::cachedAt)
                ))
                .map(Map.Entry::getKey)
                .ifPresent(ouidCache::remove);
    }

    private boolean sameText(String source, String target) {
        if (source == null || target == null) {
            return false;
        }
        return normalizeText(source).equals(normalizeText(target));
    }

    private String normalizeText(String value) {
        return value.trim()
                .replaceFirst("(?i)^c[-_\\s]*", "")
                .replaceFirst("^제(?=\\d)", "")
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .toLowerCase(Locale.ROOT);
    }

    private String requireValue(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " 값이 필요합니다.");
        }
        return value.trim();
    }

    private boolean isAll(String value) {
        return value != null && value.trim().equalsIgnoreCase("ALL");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record MatchCacheKey(String userName, String matchMode, String matchType) {
    }

    private record SummaryQuery(String matchMode, String matchType) {
    }

    private record MatchCacheEntry(
            List<MatchDto> matches,
            Instant cachedAt,
            Instant expiresAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private record DetailCacheEntry(
            MatchDetailDto detail,
            Instant cachedAt,
            Instant expiresAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private record ClanCacheEntry(
            String clanName,
            Instant cachedAt,
            Instant expiresAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private record OuidCacheEntry(
            String ouid,
            Instant cachedAt,
            Instant expiresAt) {

        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record FavoriteMatchSummaryRefresh(
            MatchSummaryResponseDto summary,
            List<Integer> activeQueryIndexes,
            boolean fullDiscovery) {
    }
}
