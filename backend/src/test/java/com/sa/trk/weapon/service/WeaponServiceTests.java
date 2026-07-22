package com.sa.trk.weapon.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.match.dto.HeadshotStatsDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.player.service.PlayerService;

class WeaponServiceTests {

    @Mock
    private PlayerService playerService;

    @Mock
    private MatchService matchService;

    private WeaponService weaponService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        weaponService = new WeaponService(playerService, matchService);
    }

    @Test
    void buildsWeaponClassAndAccuracyAnalysis() {
        OuidResponseDto ouid = new OuidResponseDto();
        ouid.setOuid("ouid-1");

        UserRecentInfoDto recent = new UserRecentInfoDto();
        recent.setRecent_assault_rate(50.3);
        recent.setRecent_sniper_rate(61.0);
        recent.setRecent_special_rate(50.0);

        HeadshotStatsDto headshots = new HeadshotStatsDto();
        headshots.setSampleMatchCount(10);
        headshots.setTotalKills(80);
        headshots.setTotalHeadshots(28);
        headshots.setHeadshotRate(35.0);
        headshots.setAverageHeadshots(2.8);

        when(playerService.getOuid("alpha")).thenReturn(ouid);
        when(playerService.getUserRecentInfo("ouid-1")).thenReturn(recent);
        when(matchService.getHeadshotStats("alpha")).thenReturn(headshots);

        var response = weaponService.getWeaponStats("alpha");

        assertThat(response.getPrimaryClass()).isEqualTo("저격");
        assertThat(response.getPrimaryGap()).isEqualTo(10.7);
        assertThat(response.getCombatType()).isEqualTo("저격 특화형");
        assertThat(response.getSpecializationIndex()).isEqualTo(18.0);
        assertThat(response.getHeadshotRate()).isEqualTo(35.0);
        assertThat(response.getAccuracyType()).isEqualTo("정밀형");
        assertThat(response.getSampleMatchCount()).isEqualTo(10);
    }
}
