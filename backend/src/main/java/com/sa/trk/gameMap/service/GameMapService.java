package com.sa.trk.gameMap.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.sa.trk.gameMap.dto.GameMapResponseDto;
import com.sa.trk.gameMap.dto.GameMapStatsItemDto;
import com.sa.trk.gameMap.dto.GameMapStatsResponseDto;
import com.sa.trk.gameMap.dto.GameMapStatsStatusDto;
import com.sa.trk.match.dto.MatchDetailResponseDto;
import com.sa.trk.match.dto.MatchListResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.nexon.dto.MatchDto;

@Service
public class GameMapService {

    private static final int MAP_STATS_SAMPLE_SIZE = 20;
    private static final int MAX_STATS_CACHE_ENTRIES = 500;

    private final MatchService matchService;
    private final Map<String, StatsCacheEntry> statsCache = new ConcurrentHashMap<>();

    public GameMapService(MatchService matchService) {
        this.matchService = matchService;
    }

    public GameMapResponseDto getMapMatches(
            String userName,
            String mapName,
            Integer page,
            String scope,
            String matchType) {
        MatchListResponseDto matchListResponse = matchService.getMatches(
                userName,
                page,
                scope,
                matchType,
                "ALL",
                mapName
        );

        List<MatchDetailResponseDto> filteredMatches = new ArrayList<>();

        for (MatchDto match : matchListResponse.getMatches()) {
            MatchDetailResponseDto detail = matchService.getMatchDetail(match.getMatch_id());

            filteredMatches.add(detail);
        }

        GameMapResponseDto responseDto = new GameMapResponseDto();
        responseDto.setUserName(userName);
        responseDto.setMapName(mapName);
        responseDto.setMatches(filteredMatches);

        return responseDto;
    }

    public GameMapStatsResponseDto getMapStats(String userName) {
        String normalizedUserName = userName == null ? null : userName.trim();
        String cacheKey = normalizedUserName == null ? "" : normalizedUserName.toLowerCase(Locale.ROOT);
        String currentVersion = Objects.requireNonNullElse(
                matchService.getRecentMatchVersion(normalizedUserName, MAP_STATS_SAMPLE_SIZE),
                ""
        );
        StatsCacheEntry cachedEntry = statsCache.get(cacheKey);
        if (cachedEntry != null && currentVersion.equals(cachedEntry.version())) {
            return cachedEntry.response();
        }

        List<MatchDto> matches = matchService.getRecentMatchesWithMap(normalizedUserName, MAP_STATS_SAMPLE_SIZE);
        Map<String, MapAccumulator> totals = new LinkedHashMap<>();

        for (MatchDto match : matches) {
            if (match == null || match.getMatch_map() == null || match.getMatch_map().isBlank()) {
                continue;
            }
            totals.computeIfAbsent(match.getMatch_map().trim(), ignored -> new MapAccumulator())
                    .add(match);
        }

        List<GameMapStatsItemDto> maps = totals.entrySet().stream()
                .map(entry -> entry.getValue().toDto(entry.getKey()))
                .sorted(Comparator
                        .comparing(GameMapStatsItemDto::getMatchCount).reversed()
                        .thenComparing(GameMapStatsItemDto::getWinRate, Comparator.reverseOrder())
                        .thenComparing(GameMapStatsItemDto::getMapName))
                .toList();

        GameMapStatsResponseDto response = new GameMapStatsResponseDto();
        response.setUserName(normalizedUserName);
        response.setVersion(currentVersion);
        response.setSampleSize(matches.size());
        response.setMaps(maps);

        if (statsCache.size() >= MAX_STATS_CACHE_ENTRIES) {
            statsCache.entrySet().stream()
                    .min(Map.Entry.comparingByValue(Comparator.comparing(StatsCacheEntry::cachedAt)))
                    .map(Map.Entry::getKey)
                    .ifPresent(statsCache::remove);
        }
        statsCache.put(cacheKey, new StatsCacheEntry(
                response,
                Instant.now(),
                currentVersion
        ));
        return response;
    }

    public GameMapStatsStatusDto getMapStatsStatus(String userName, String clientVersion) {
        String normalizedUserName = userName == null ? null : userName.trim();
        String currentVersion = Objects.requireNonNullElse(
                matchService.getRecentMatchVersion(normalizedUserName, MAP_STATS_SAMPLE_SIZE),
                ""
        );
        return new GameMapStatsStatusDto(
                currentVersion,
                currentVersion.equals(clientVersion == null ? "" : clientVersion.trim())
        );
    }

    private record StatsCacheEntry(
            GameMapStatsResponseDto response,
            Instant cachedAt,
            String version) {
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static class MapAccumulator {
        private int matchCount;
        private int winCount;
        private int drawCount;
        private int loseCount;
        private int killCount;
        private int deathCount;

        void add(MatchDto match) {
            matchCount++;
            killCount += match.getKill() == null ? 0 : match.getKill();
            deathCount += match.getDeath() == null ? 0 : match.getDeath();
            if ("1".equals(match.getMatch_result())) winCount++;
            if ("2".equals(match.getMatch_result())) loseCount++;
            if ("3".equals(match.getMatch_result())) drawCount++;
        }

        GameMapStatsItemDto toDto(String mapName) {
            int resultCount = winCount + drawCount + loseCount;
            GameMapStatsItemDto item = new GameMapStatsItemDto();
            item.setMapName(mapName);
            item.setMatchCount(matchCount);
            item.setWinCount(winCount);
            item.setDrawCount(drawCount);
            item.setLoseCount(loseCount);
            item.setWinRate(resultCount == 0 ? 0.0 : round(winCount * 100.0 / resultCount));
            item.setAverageKill(matchCount == 0 ? 0.0 : round(killCount * 1.0 / matchCount));
            item.setAverageDeath(matchCount == 0 ? 0.0 : round(deathCount * 1.0 / matchCount));
            item.setKillDeathRatio(deathCount == 0 ? round(killCount) : round(killCount * 1.0 / deathCount));
            return item;
        }
    }
}
