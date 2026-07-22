package com.sa.trk.gameMap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.match.service.MatchService;
import com.sa.trk.nexon.dto.MatchDto;

class GameMapServiceTests {

    @Mock
    private MatchService matchService;

    private GameMapService gameMapService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        gameMapService = new GameMapService(matchService);
    }

    @Test
    void aggregatesAndRanksRecentMapPerformance() {
        when(matchService.getRecentMatchVersion("alpha", 20)).thenReturn("version-1");
        when(matchService.getRecentMatchesWithMap("alpha", 20)).thenReturn(List.of(
                match("Warehouse", "1", 10, 5),
                match("Warehouse", "2", 4, 8),
                match("Provence", "1", 9, 3),
                match("", "1", 20, 1)
        ));

        var response = gameMapService.getMapStats(" alpha ");

        assertThat(response.getUserName()).isEqualTo("alpha");
        assertThat(response.getVersion()).isEqualTo("version-1");
        assertThat(response.getSampleSize()).isEqualTo(4);
        assertThat(response.getMaps()).extracting("mapName")
                .containsExactly("Warehouse", "Provence");

        var warehouse = response.getMaps().get(0);
        assertThat(warehouse.getMatchCount()).isEqualTo(2);
        assertThat(warehouse.getWinCount()).isEqualTo(1);
        assertThat(warehouse.getLoseCount()).isEqualTo(1);
        assertThat(warehouse.getWinRate()).isEqualTo(50.0);
        assertThat(warehouse.getAverageKill()).isEqualTo(7.0);
        assertThat(warehouse.getAverageDeath()).isEqualTo(6.5);
        assertThat(warehouse.getKillDeathRatio()).isEqualTo(1.1);
    }

    private MatchDto match(String mapName, String result, int kill, int death) {
        MatchDto match = new MatchDto();
        match.setMatch_map(mapName);
        match.setMatch_result(result);
        match.setKill(kill);
        match.setDeath(death);
        return match;
    }
}
