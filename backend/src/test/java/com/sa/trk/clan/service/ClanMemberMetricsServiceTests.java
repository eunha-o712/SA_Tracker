package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.player.service.PlayerService;

class ClanMemberMetricsServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private WeaponService weaponService;

    @Mock
    private PlayerService playerService;

    private ClanMemberMetricsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClanMemberMetricsService(clanMemberRepository, matchService, weaponService);
        when(clanMemberRepository.save(any(ClanMember.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void refreshesAndStoresTeamBuildingMetrics() {
        ClanMember member = member("힉");
        when(matchService.getMatchSummary("힉")).thenReturn(summary());
        when(weaponService.getWeaponStats("힉")).thenReturn(weapon());

        ClanMember refreshed = service.refreshMember(member);

        assertThat(refreshed.getStatsAvailable()).isTrue();
        assertThat(refreshed.getStatsMatchCount()).isEqualTo(20);
        assertThat(refreshed.getStatsWinRate()).isEqualTo(60.0);
        assertThat(refreshed.getStatsKillDeathRatio()).isEqualTo(2.0);
        assertThat(refreshed.getStatsAverageKill()).isEqualTo(10.0);
        assertThat(refreshed.getStatsPrimaryClass()).isEqualTo("저격");
        assertThat(refreshed.getStatsPowerScore()).isGreaterThan(0);
        assertThat(refreshed.getStatsUpdatedAt()).isNotNull();
    }

    @Test
    void keepsPreviousSnapshotWhenRefreshFails() {
        ClanMember member = member("힉");
        member.setStatsAvailable(true);
        member.setStatsMatchCount(15);
        member.setStatsPowerScore(55.5);
        member.setStatsUpdatedAt(java.time.LocalDateTime.of(2026, 7, 30, 12, 0));
        when(matchService.getMatchSummary("힉")).thenThrow(new IllegalStateException("temporary failure"));

        ClanMember refreshed = service.refreshMember(member);

        assertThat(refreshed.getStatsAvailable()).isTrue();
        assertThat(refreshed.getStatsMatchCount()).isEqualTo(15);
        assertThat(refreshed.getStatsPowerScore()).isEqualTo(55.5);
        assertThat(refreshed.getStatsUpdatedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 7, 30, 12, 0));
    }

    @Test
    void refreshesRenamedMemberByStoredOuid() {
        ClanMemberMetricsService ouidService = new ClanMemberMetricsService(
                clanMemberRepository,
                matchService,
                weaponService,
                playerService
        );
        ClanMember member = member("oldName");
        member.setOuid("ouid-player");
        UserBasicDto basic = new UserBasicDto();
        basic.setUser_name("newName");
        basic.setClan_name("testClan");
        when(playerService.getUserBasic("ouid-player")).thenReturn(basic);
        when(matchService.getMatchSummaryByOuid("newName", "ouid-player")).thenReturn(summary());
        when(weaponService.getWeaponStatsByOuid("newName", "ouid-player")).thenReturn(weapon());

        ClanMember refreshed = ouidService.refreshMember(member);

        assertThat(refreshed.getUserName()).isEqualTo("newName");
        assertThat(refreshed.getClanName()).isEqualTo("testClan");
        assertThat(refreshed.getStatsAvailable()).isTrue();
    }

    private ClanMember member(String userName) {
        ClanMember member = new ClanMember();
        member.setId(1L);
        member.setUserName(userName);
        member.setClanName("다봄");
        return member;
    }

    private MatchSummaryResponseDto summary() {
        MatchSummaryItemDto item = new MatchSummaryItemDto();
        item.setKey("CLAN");
        item.setMatchCount(20);
        item.setWinCount(12);
        item.setDrawCount(2);
        item.setLoseCount(6);
        item.setWinRate(60.0);
        item.setAverageKill(10.0);
        item.setAverageDeath(5.0);
        MatchSummaryResponseDto response = new MatchSummaryResponseDto();
        response.setSummaries(List.of(item));
        return response;
    }

    private WeaponStatsResponseDto weapon() {
        WeaponStatsResponseDto response = new WeaponStatsResponseDto();
        response.setPrimaryClass("저격");
        response.setCombatType("저격 특화형");
        response.setAssaultRate(10.0);
        response.setSniperRate(70.0);
        response.setSpecialRate(5.0);
        return response;
    }
}
