package com.sa.trk.ai.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

import org.springframework.stereotype.Service;

import com.sa.trk.ai.dto.AiAnalysisStatusDto;
import com.sa.trk.ai.dto.RecordRoomAiResponseDto;
import com.sa.trk.ai.entity.AiAnalysisCache;
import com.sa.trk.ai.repository.AiAnalysisCacheRepository;
import com.sa.trk.gameMap.dto.GameMapStatsResponseDto;
import com.sa.trk.gameMap.service.GameMapService;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.player.dto.PlayerResponseDto;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class RecordRoomAiService {

    private static final int MAX_MAPS = 5;
    private static final int ANALYSIS_LOCK_COUNT = 64;

    private final PlayerService playerService;
    private final MatchService matchService;
    private final WeaponService weaponService;
    private final GameMapService gameMapService;
    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final AiAnalysisCacheRepository analysisCacheRepository;
    private final Object[] analysisLocks = createAnalysisLocks();

    public RecordRoomAiService(
            PlayerService playerService,
            MatchService matchService,
            WeaponService weaponService,
            GameMapService gameMapService,
            OpenAiClient openAiClient,
            ObjectMapper objectMapper,
            AiAnalysisCacheRepository analysisCacheRepository) {
        this.playerService = playerService;
        this.matchService = matchService;
        this.weaponService = weaponService;
        this.gameMapService = gameMapService;
        this.openAiClient = openAiClient;
        this.objectMapper = objectMapper;
        this.analysisCacheRepository = analysisCacheRepository;
    }

    public RecordRoomAiResponseDto analyze(String userName) {
        AnalysisSnapshot snapshot = loadAnalysisSnapshot(userName);
        Object analysisLock = analysisLocks[
                Math.floorMod(snapshot.cacheKey().hashCode(), analysisLocks.length)
        ];

        synchronized (analysisLock) {
            RecordRoomAiResponseDto cachedResponse = findCachedResponse(
                    snapshot.cacheKey(),
                    snapshot.snapshotHash()
            );
            if (cachedResponse != null) {
                cachedResponse.setAiGenerated(false);
                return cachedResponse;
            }

            RecordRoomAiResponseDto response = generateResponse(
                    snapshot.displayName(),
                    snapshot.snapshotJson(),
                    snapshot.matchSummary(),
                    snapshot.mapStats()
            );
            saveCachedResponse(snapshot.cacheKey(), snapshot.snapshotHash(), response);
            return response;
        }
    }

    public AiAnalysisStatusDto getStatus(String userName) {
        AnalysisSnapshot snapshot = loadAnalysisSnapshot(userName);
        boolean upToDate = analysisCacheRepository.findById(snapshot.cacheKey())
                .map(cache -> snapshot.snapshotHash().equals(cache.getSnapshotHash()))
                .orElse(false);
        return new AiAnalysisStatusDto(snapshot.snapshotHash(), upToDate);
    }

    private AnalysisSnapshot loadAnalysisSnapshot(String userName) {
        String normalizedUserName = requireUserName(userName);
        PlayerResponseDto player = playerService.getPlayer(normalizedUserName);
        String displayName = firstNonBlank(player.getUserName(), normalizedUserName);

        MatchSummaryResponseDto matchSummary = getSafely(() -> matchService.getMatchSummary(displayName));
        WeaponStatsResponseDto weaponStats = getSafely(() -> weaponService.getWeaponStats(displayName));
        GameMapStatsResponseDto mapStats = getSafely(() -> gameMapService.getMapStats(displayName));

        Map<String, Object> snapshot = createSnapshot(displayName, player, matchSummary, weaponStats, mapStats);
        String snapshotJson = serializeSnapshot(snapshot);
        String snapshotHash = createSnapshotHash(snapshotJson);
        String cacheKey = normalizedUserName.toLowerCase(Locale.ROOT);
        return new AnalysisSnapshot(
                cacheKey,
                displayName,
                snapshotJson,
                snapshotHash,
                matchSummary,
                mapStats
        );
    }

    private RecordRoomAiResponseDto generateResponse(
            String displayName,
            String snapshotJson,
            MatchSummaryResponseDto matchSummary,
            GameMapStatsResponseDto mapStats) {
        String aiText = openAiClient.createTextResponse(createPrompt(snapshotJson));

        RecordRoomAiResponseDto response = parseAiResponse(aiText);
        response.setUserName(displayName);
        response.setModel(openAiClient.resolveModel());
        response.setGeneratedAt(Instant.now());
        response.setSampleSize(resolveSampleSize(matchSummary, mapStats));
        return response;
    }

    private Map<String, Object> createSnapshot(
            String displayName,
            PlayerResponseDto player,
            MatchSummaryResponseDto matchSummary,
            WeaponStatsResponseDto weaponStats,
            GameMapStatsResponseDto mapStats) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("userName", displayName);
        snapshot.put("basic", Map.of(
                "createdAt", value(player.getBasic() == null ? null : player.getBasic().getUser_date_create()),
                "title", value(player.getBasic() == null ? null : player.getBasic().getTitle_name()),
                "clan", value(player.getBasic() == null ? null : player.getBasic().getClan_name()),
                "mannerGrade", value(player.getBasic() == null ? null : player.getBasic().getManner_grade())
        ));
        snapshot.put("rank", player.getRank());
        snapshot.put("tier", player.getTier());
        snapshot.put("recentAccountStats", player.getRecent());
        snapshot.put("weaponRates", weaponStats);
        snapshot.put("matchSummary", matchSummary);
        snapshot.put("mapStats", trimMapStats(mapStats));
        return snapshot;
    }

    private Map<String, Object> trimMapStats(GameMapStatsResponseDto mapStats) {
        if (mapStats == null) {
            return Map.of("available", false);
        }

        Map<String, Object> trimmed = new LinkedHashMap<>();
        trimmed.put("available", true);
        trimmed.put("sampleSize", mapStats.getSampleSize());
        trimmed.put("maps", mapStats.getMaps() == null
                ? List.of()
                : mapStats.getMaps().stream().limit(MAX_MAPS).toList());
        return trimmed;
    }

    private String serializeSnapshot(Map<String, Object> snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JacksonException exception) {
            throw new IllegalStateException("AI 분석 데이터를 구성할 수 없습니다.", exception);
        }
    }

    private String createPrompt(String dataJson) {
        return """
                당신은 FPS 게임 Sudden Attack 전적을 분석하는 한국어 코치입니다.
                아래 JSON 데이터만 근거로 플레이어의 레코드룸 AI 분석을 작성하세요.
                데이터에 없는 사실은 추측하지 마세요. 수치가 부족하면 '표본이 제한적'이라고 표현하세요.
                응답은 마크다운 없이 순수 JSON만 반환하세요.
                JSON 스키마:
                {
                  "summary": "2문장 이내의 핵심 분석",
                  "playStyle": "짧은 플레이스타일 라벨",
                  "strengths": ["강점 1", "강점 2", "강점 3"],
                  "risks": ["주의점 1", "주의점 2"],
                  "recommendations": ["추천 1", "추천 2", "추천 3"]
                }

                분석 데이터:
                %s
                """.formatted(dataJson);
    }

    private String createSnapshotHash(String snapshotJson) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(snapshotJson.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("AI 분석 캐시 해시를 생성할 수 없습니다.", exception);
        }
    }

    private static Object[] createAnalysisLocks() {
        Object[] locks = new Object[ANALYSIS_LOCK_COUNT];
        Arrays.setAll(locks, index -> new Object());
        return locks;
    }

    private RecordRoomAiResponseDto findCachedResponse(String cacheKey, String snapshotHash) {
        return analysisCacheRepository.findById(cacheKey)
                .filter(cache -> snapshotHash.equals(cache.getSnapshotHash()))
                .map(this::deserializeCachedResponse)
                .orElse(null);
    }

    private RecordRoomAiResponseDto deserializeCachedResponse(AiAnalysisCache cache) {
        try {
            return objectMapper.readValue(cache.getResponseJson(), RecordRoomAiResponseDto.class);
        } catch (JacksonException exception) {
            analysisCacheRepository.delete(cache);
            return null;
        }
    }

    private void saveCachedResponse(
            String cacheKey,
            String snapshotHash,
            RecordRoomAiResponseDto response) {
        try {
            AiAnalysisCache cache = analysisCacheRepository.findById(cacheKey)
                    .orElseGet(AiAnalysisCache::new);
            cache.setNormalizedUserName(cacheKey);
            cache.setSnapshotHash(snapshotHash);
            cache.setResponseJson(objectMapper.writeValueAsString(response));
            cache.setUpdatedAt(Instant.now());
            analysisCacheRepository.save(cache);
        } catch (JacksonException exception) {
            throw new IllegalStateException("AI 분석 결과를 저장할 수 없습니다.", exception);
        }
    }

    private RecordRoomAiResponseDto parseAiResponse(String aiText) {
        String normalized = stripCodeFence(aiText);
        RecordRoomAiResponseDto response = new RecordRoomAiResponseDto();

        try {
            JsonNode root = objectMapper.readTree(normalized);
            response.setSummary(text(root, "summary", normalized));
            response.setPlayStyle(text(root, "playStyle", "AI 분석"));
            response.setStrengths(list(root.path("strengths")));
            response.setRisks(list(root.path("risks")));
            response.setRecommendations(list(root.path("recommendations")));
        } catch (JacksonException exception) {
            response.setSummary(normalized);
            response.setPlayStyle("AI 분석");
            response.setStrengths(List.of());
            response.setRisks(List.of());
            response.setRecommendations(List.of());
        }

        return response;
    }

    private List<String> list(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (!node.isArray()) {
            return values;
        }
        for (JsonNode item : node) {
            String value = item.asText("");
            if (!value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private String text(JsonNode root, String fieldName, String fallback) {
        String value = root.path(fieldName).asText("");
        return value.isBlank() ? fallback : value.trim();
    }

    private String stripCodeFence(String value) {
        String text = value == null ? "" : value.trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }
        return text.trim();
    }

    private Integer resolveSampleSize(MatchSummaryResponseDto matchSummary, GameMapStatsResponseDto mapStats) {
        if (matchSummary != null && matchSummary.getSummaries() != null) {
            return matchSummary.getSummaries().stream()
                    .filter(item -> "RECENT".equals(item.getKey()))
                    .findFirst()
                    .map(item -> item.getMatchCount() == null ? 0 : item.getMatchCount())
                    .orElse(0);
        }
        return mapStats == null || mapStats.getSampleSize() == null ? 0 : mapStats.getSampleSize();
    }

    private <T> T getSafely(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception exception) {
            return null;
        }
    }

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private String requireUserName(String userName) {
        if (isBlank(userName)) {
            throw new IllegalArgumentException("닉네임 값이 필요합니다.");
        }
        return userName.trim();
    }

    private String value(String value) {
        return isBlank(value) ? "-" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private record AnalysisSnapshot(
            String cacheKey,
            String displayName,
            String snapshotJson,
            String snapshotHash,
            MatchSummaryResponseDto matchSummary,
            GameMapStatsResponseDto mapStats) {
    }
}
