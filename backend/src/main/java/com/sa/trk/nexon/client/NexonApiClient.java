package com.sa.trk.nexon.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.sa.trk.config.NexonProperties;
import com.sa.trk.nexon.dto.GradeDto;
import com.sa.trk.nexon.dto.LogoDto;
import com.sa.trk.nexon.dto.MatchDetailDto;
import com.sa.trk.nexon.dto.MatchDto;
import com.sa.trk.nexon.dto.MatchResponseDto;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.SeasonGradeDto;
import com.sa.trk.nexon.dto.TierDto;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.nexon.dto.UserTierDto;

@Component
public class NexonApiClient {

    private static final Logger log = LoggerFactory.getLogger(NexonApiClient.class);
    private static final long MIN_REQUEST_INTERVAL_MS = 350L;
    private static final int MAX_RETRY_COUNT = 3;
    private static final Duration OUID_CACHE_DURATION = Duration.ofHours(6);

    private final RestTemplate restTemplate;
    private final NexonProperties nexonProperties;
    private final Map<String, OuidCacheEntry> ouidCache = new ConcurrentHashMap<>();
    private long lastRequestAtMs;

    public NexonApiClient(
            RestTemplate restTemplate,
            NexonProperties nexonProperties) {
        this.restTemplate = restTemplate;
        this.nexonProperties = nexonProperties;
    }

    public synchronized OuidResponseDto getOuid(String userName) {
        String cacheKey = userName == null
                ? ""
                : userName.trim().toLowerCase(Locale.ROOT);
        OuidCacheEntry cachedEntry = ouidCache.get(cacheKey);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            OuidResponseDto cachedResponse = new OuidResponseDto();
            cachedResponse.setOuid(cachedEntry.ouid());
            log.debug("Nexon OUID cache hit");
            return cachedResponse;
        }

        URI uri = createUri(
                "/suddenattack/v1/id",
                "user_name",
                userName
        );

        OuidResponseDto response = exchange(
                uri,
                OuidResponseDto.class
        ).getBody();

        if (response != null && response.getOuid() != null && !response.getOuid().isBlank()) {
            ouidCache.put(
                    cacheKey,
                    new OuidCacheEntry(
                            response.getOuid(),
                            Instant.now().plus(OUID_CACHE_DURATION)
                    )
            );
        }
        return response;
    }

    public UserBasicDto getUserBasic(String ouid) {
        URI uri = createUri(
                "/suddenattack/v1/user/basic",
                "ouid",
                ouid
        );

        return exchange(
                uri,
                UserBasicDto.class
        ).getBody();
    }

    public UserRankDto getUserRank(String ouid) {
        URI uri = createUri(
                "/suddenattack/v1/user/rank",
                "ouid",
                ouid
        );

        return exchange(
                uri,
                UserRankDto.class
        ).getBody();
    }

    public UserTierDto getUserTier(String ouid) {
        URI uri = createUri(
                "/suddenattack/v1/user/tier",
                "ouid",
                ouid
        );

        return exchange(
                uri,
                UserTierDto.class
        ).getBody();
    }

    public UserRecentInfoDto getUserRecentInfo(String ouid) {
        URI uri = createUri(
                "/suddenattack/v1/user/recent-info",
                "ouid",
                ouid
        );

        return exchange(
                uri,
                UserRecentInfoDto.class
        ).getBody();
    }

    public List<MatchDto> getMatches(
            String ouid,
            String matchMode,
            String matchType) {

        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/suddenattack/v1/match")
                .queryParam("ouid", ouid)
                .queryParam("match_mode", matchMode)
                .queryParam("match_type", matchType)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        ResponseEntity<MatchResponseDto> response =
                exchange(uri, MatchResponseDto.class);

        MatchResponseDto body = response.getBody();

        if (body == null || body.getMatch() == null) {
            return List.of();
        }

        return body.getMatch();
    }

    public MatchDetailDto getMatchDetail(String matchId) {
        URI uri = createUri(
                "/suddenattack/v1/match-detail",
                "match_id",
                matchId
        );

        return exchange(
                uri,
                MatchDetailDto.class
        ).getBody();
    }

    public LogoDto getLogo() {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/static/suddenattack/meta/logo")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        return exchange(
                uri,
                LogoDto.class
        ).getBody();
    }

    public List<GradeDto> getGrades() {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/static/suddenattack/meta/grade")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        ResponseEntity<GradeDto[]> response =
                exchange(uri, GradeDto[].class);

        GradeDto[] body = response.getBody();

        return body != null
                ? Arrays.asList(body)
                : List.of();
    }

    public List<SeasonGradeDto> getSeasonGrades() {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/static/suddenattack/meta/season_grade")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        ResponseEntity<SeasonGradeDto[]> response =
                exchange(uri, SeasonGradeDto[].class);

        SeasonGradeDto[] body = response.getBody();

        return body != null
                ? Arrays.asList(body)
                : List.of();
    }

    public List<TierDto> getTiers() {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/static/suddenattack/meta/tier")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        ResponseEntity<TierDto[]> response =
                exchange(uri, TierDto[].class);

        TierDto[] body = response.getBody();

        return body != null
                ? Arrays.asList(body)
                : List.of();
    }

    private URI createUri(
            String path,
            String parameterName,
            String parameterValue) {

        return UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path(path)
                .queryParam(parameterName, parameterValue)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
    }

    private synchronized <T> ResponseEntity<T> exchange(
            URI uri,
            Class<T> responseType) {

        HttpEntity<String> entity =
                new HttpEntity<>(createHeaders());

        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            waitForRequestSlot();
            try {
                log.info("Nexon API GET {} (attempt {}/{})",
                        uri.getPath(), attempt, MAX_RETRY_COUNT);
                ResponseEntity<T> response = restTemplate.exchange(
                        uri,
                        HttpMethod.GET,
                        entity,
                        responseType
                );
                log.info("Nexon API {} -> {}", uri.getPath(), response.getStatusCode().value());
                return response;
            } catch (HttpClientErrorException.TooManyRequests exception) {
                log.warn("Nexon API rate limit on {} (attempt {}/{})",
                        uri.getPath(), attempt, MAX_RETRY_COUNT);
                if (attempt == MAX_RETRY_COUNT) {
                    throw exception;
                }
                sleep(1_500L * attempt);
            }
        }

        throw new IllegalStateException("Nexon API request retry loop ended unexpectedly");
    }

    private void waitForRequestSlot() {
        long waitMs = MIN_REQUEST_INTERVAL_MS
                - (System.currentTimeMillis() - lastRequestAtMs);
        if (waitMs > 0) {
            sleep(waitMs);
        }
        lastRequestAtMs = System.currentTimeMillis();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nexon API request interrupted", exception);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "x-nxopen-api-key",
                nexonProperties.getKey()
        );

        return headers;
    }

    private record OuidCacheEntry(String ouid, Instant expiresAt) {
        private boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
