package com.sa.trk.nexon.client;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.net.URI;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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

import org.springframework.web.util.UriComponentsBuilder;


@Component
public class NexonApiClient {

    private final RestTemplate restTemplate;
    private final NexonProperties nexonProperties;

    public NexonApiClient(RestTemplate restTemplate, NexonProperties nexonProperties) {
        this.restTemplate = restTemplate;
        this.nexonProperties = nexonProperties;
    }

    public OuidResponseDto getOuid(String userName) {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/suddenattack/v1/id")
                .queryParam("user_name", userName)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<OuidResponseDto> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                OuidResponseDto.class
        );

        return response.getBody();
    }
    public UserBasicDto getUserBasic(String ouid) {
        String url = nexonProperties.getBaseUrl() + "/suddenattack/v1/user/basic?ouid=" + ouid;

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<UserBasicDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserBasicDto.class
        );

        return response.getBody();
    }

    public UserRankDto getUserRank(String ouid) {
        String url = nexonProperties.getBaseUrl() + "/suddenattack/v1/user/rank?ouid=" + ouid;

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<UserRankDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserRankDto.class
        );

        return response.getBody();
    }

    public UserTierDto getUserTier(String ouid) {
        String url = nexonProperties.getBaseUrl() + "/suddenattack/v1/user/tier?ouid=" + ouid;

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<UserTierDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserTierDto.class
        );

        return response.getBody();
    }

    public UserRecentInfoDto getUserRecentInfo(String ouid) {
        String url = nexonProperties.getBaseUrl() + "/suddenattack/v1/user/recent-info?ouid=" + ouid;

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<UserRecentInfoDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                UserRecentInfoDto.class
        );

        return response.getBody();
    }

    public List<MatchDto> getMatches(String ouid, String matchMode, String matchType) {
        URI uri = UriComponentsBuilder
                .fromUriString(nexonProperties.getBaseUrl())
                .path("/suddenattack/v1/match")
                .queryParam("ouid", ouid)
                .queryParam("match_mode", matchMode)
                .queryParam("match_type", matchType)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<MatchResponseDto> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                entity,
                MatchResponseDto.class
        );

        MatchResponseDto body = response.getBody();
        return body != null ? body.getMatch() : List.of();
    }

    public MatchDetailDto getMatchDetail(String matchId) {
        String url = nexonProperties.getBaseUrl() + "/suddenattack/v1/match-detail?match_id=" + matchId;

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<MatchDetailDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                MatchDetailDto.class
        );

        return response.getBody();
    }

    public LogoDto getLogo() {
        String url = nexonProperties.getBaseUrl() + "/static/suddenattack/meta/logo";

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<LogoDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                LogoDto.class
        );

        return response.getBody();
    }

    public List<GradeDto> getGrades() {
        String url = nexonProperties.getBaseUrl() + "/static/suddenattack/meta/grade";

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<GradeDto[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                GradeDto[].class
        );

        GradeDto[] body = response.getBody();
        return body != null ? Arrays.asList(body) : List.of();
    }

    public List<SeasonGradeDto> getSeasonGrades() {
        String url = nexonProperties.getBaseUrl() + "/static/suddenattack/meta/season_grade";

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<SeasonGradeDto[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                SeasonGradeDto[].class
        );

        SeasonGradeDto[] body = response.getBody();
        return body != null ? Arrays.asList(body) : List.of();
    }

    public List<TierDto> getTiers() {
        String url = nexonProperties.getBaseUrl() + "/static/suddenattack/meta/tier";

        HttpEntity<String> entity = new HttpEntity<>(createHeaders());

        ResponseEntity<TierDto[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                TierDto[].class
        );

        TierDto[] body = response.getBody();
        return body != null ? Arrays.asList(body) : List.of();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-nxopen-api-key", nexonProperties.getKey());
        return headers;
    }
}