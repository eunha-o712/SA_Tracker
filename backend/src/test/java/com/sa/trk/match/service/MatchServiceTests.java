package com.sa.trk.match.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.MatchDetailDto;
import com.sa.trk.nexon.dto.MatchDetailItemDto;
import com.sa.trk.nexon.dto.MatchDto;
import com.sa.trk.nexon.dto.OuidResponseDto;

class MatchServiceTests {

    @Test
    void discoversOnlyQueryRangesThatContainMatches() {
        NexonApiClient client = mock(NexonApiClient.class);
        AtomicInteger requestIndex = new AtomicInteger();
        when(client.getMatches(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    int index = requestIndex.getAndIncrement();
                    return List.of(0, 3, 7).contains(index)
                            ? List.of(match("match-" + index))
                            : List.of();
                });
        MatchService service = new MatchService(client);

        var result = service.getFavoriteMatchSummaryByOuid(
                "player",
                "ouid-player",
                List.of(),
                true
        );

        assertThat(result.activeQueryIndexes()).containsExactly(0, 3, 7);
        assertThat(result.fullDiscovery()).isTrue();
        verify(client, times(10)).getMatches(anyString(), anyString(), anyString());
    }

    @Test
    void refreshesOnlyPreviouslyActiveQueryRanges() {
        NexonApiClient client = mock(NexonApiClient.class);
        when(client.getMatches(anyString(), anyString(), anyString()))
                .thenReturn(List.of(match("match")));
        MatchService service = new MatchService(client);

        var result = service.getFavoriteMatchSummaryByOuid(
                "player",
                "ouid-player",
                List.of(0, 3, 7),
                false
        );

        assertThat(result.activeQueryIndexes()).containsExactly(0, 3, 7);
        assertThat(result.fullDiscovery()).isFalse();
        verify(client, times(3)).getMatches(anyString(), anyString(), anyString());
    }

    @Test
    void resolvesMatchPageQueriesToFavoriteRefreshRanges() {
        MatchService service = new MatchService(mock(NexonApiClient.class));

        assertThat(service.resolveFavoriteQueryIndexes("RECENT", "ALL", "일반전"))
                .containsExactly(0, 1, 2);
        assertThat(service.resolveFavoriteQueryIndexes("CLAN", "폭파미션", "퀵매치 클랜전"))
                .containsExactly(5);
        assertThat(service.resolveFavoriteQueryIndexes("BOMB", "폭파미션", "토너먼트"))
                .isEmpty();
    }

    @Test
    void failsFastInsteadOfRepeatingRateLimitAcrossEverySummaryRange() {
        NexonApiClient client = mock(NexonApiClient.class);
        OuidResponseDto ouid = new OuidResponseDto();
        ouid.setOuid("ouid-player");
        when(client.getOuid("player")).thenReturn(ouid);
        when(client.getMatches(anyString(), anyString(), anyString()))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Too Many Requests",
                        HttpHeaders.EMPTY,
                        new byte[0],
                        null
                ));
        MatchService service = new MatchService(client);

        assertThatThrownBy(() -> service.getMatchSummary("player"))
                .isInstanceOf(HttpClientErrorException.TooManyRequests.class);

        verify(client, times(1)).getMatches(anyString(), anyString(), anyString());
    }

    @Test
    void identifiesDisguisedNicknameByTheOuidMatchRecord() {
        NexonApiClient client = mock(NexonApiClient.class);
        MatchDto targetMatch = match("match-disguised");
        targetMatch.setMatch_result("1");
        when(client.getMatches(anyString(), anyString(), anyString()))
                .thenReturn(List.of(targetMatch));

        MatchDetailItemDto target = detailPlayer("actual-name", 10, 8, 2, "1", 4);
        MatchDetailItemDto other = detailPlayer("other-player", 12, 8, 2, "1", 1);
        MatchDetailDto detail = new MatchDetailDto();
        detail.setMatch_id("match-disguised");
        detail.setMatch_detail(List.of(target, other));
        when(client.getMatchDetail("match-disguised")).thenReturn(detail);

        MatchService service = new MatchService(client);
        var stats = service.getHeadshotStatsByOuid("disguised-name", "ouid-player");

        assertThat(stats.getSampleMatchCount()).isEqualTo(1);
        assertThat(stats.getTotalKills()).isEqualTo(10);
        assertThat(stats.getTotalHeadshots()).isEqualTo(4);
        assertThat(stats.getHeadshotRate()).isEqualTo(40.0);
    }

    private MatchDto match(String id) {
        MatchDto match = new MatchDto();
        match.setMatch_id(id);
        match.setDate_match("2026-08-06T18:00:00");
        match.setKill(10);
        match.setDeath(8);
        match.setAssist(2);
        return match;
    }

    private MatchDetailItemDto detailPlayer(
            String userName,
            int kill,
            int death,
            int assist,
            String result,
            int headshot) {
        MatchDetailItemDto player = new MatchDetailItemDto();
        player.setUser_name(userName);
        player.setKill(kill);
        player.setDeath(death);
        player.setAssist(assist);
        player.setMatch_result(result);
        player.setHeadshot(headshot);
        return player;
    }
}
