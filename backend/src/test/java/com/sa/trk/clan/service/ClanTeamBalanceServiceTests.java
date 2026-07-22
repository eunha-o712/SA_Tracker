package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.clan.dto.ClanTeamBalanceRequest;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;
import com.sa.trk.match.dto.MatchSummaryItemDto;
import com.sa.trk.match.dto.MatchSummaryResponseDto;
import com.sa.trk.match.service.MatchService;
import com.sa.trk.weapon.dto.WeaponStatsResponseDto;
import com.sa.trk.weapon.service.WeaponService;

class ClanTeamBalanceServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private MatchService matchService;

    @Mock
    private WeaponService weaponService;

    private ClanTeamBalanceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClanTeamBalanceService(clanMemberRepository, matchService, weaponService);
    }

    @Test
    void balancesPowerAndDistributesSnipersAcrossTeams() {
        List<ClanMember> members = List.of(
                member(1L, "alpha"),
                member(2L, "bravo"),
                member(3L, "charlie"),
                member(4L, "delta")
        );
        when(clanMemberRepository.findAllByOwnerIdAndIdIn(7L, List.of(1L, 2L, 3L, 4L)))
                .thenReturn(members);
        stubMetrics("alpha", 70, 2.0, 14, "저격");
        stubMetrics("bravo", 50, 1.0, 8, "저격");
        stubMetrics("charlie", 65, 1.8, 13, "돌격");
        stubMetrics("delta", 45, 0.9, 7, "돌격");

        var response = service.balance(7L, new ClanTeamBalanceRequest(List.of(1L, 2L, 3L, 4L), 2, 0));

        assertThat(response.teams()).hasSize(2);
        assertThat(response.teams()).allSatisfy(team -> {
            assertThat(team.members()).hasSize(2);
            assertThat(team.roleCounts()).containsEntry("저격", 1);
            assertThat(team.roleCounts()).containsEntry("돌격", 1);
        });
        assertThat(response.balanceScore()).isGreaterThanOrEqualTo(90);
        verify(clanMemberRepository).findAllByOwnerIdAndIdIn(7L, List.of(1L, 2L, 3L, 4L));
    }

    @Test
    void rejectsMembersOutsideTheLoggedInUsersRoster() {
        when(clanMemberRepository.findAllByOwnerIdAndIdIn(7L, List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(member(1L, "alpha"), member(2L, "bravo"), member(3L, "charlie")));

        assertThatThrownBy(() -> service.balance(
                7L,
                new ClanTeamBalanceRequest(List.of(1L, 2L, 3L, 4L), 2, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 클랜 로스터");
        verifyNoInteractions(matchService, weaponService);
    }

    @Test
    void requiresPlayerCountToBeDivisibleByTeamSize() {
        assertThatThrownBy(() -> service.balance(
                7L,
                new ClanTeamBalanceRequest(List.of(1L, 2L, 3L, 4L, 5L), 2, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정확히 나누어져야");
        verifyNoInteractions(clanMemberRepository, matchService, weaponService);
    }

    @Test
    void dividesNinePlayersIntoThreeTeamsOfThree() {
        List<ClanMember> members = List.of(
                member(1L, "player1"), member(2L, "player2"), member(3L, "player3"),
                member(4L, "player4"), member(5L, "player5"), member(6L, "player6"),
                member(7L, "player7"), member(8L, "player8"), member(9L, "player9")
        );
        List<Long> ids = members.stream().map(ClanMember::getId).toList();
        when(clanMemberRepository.findAllByOwnerIdAndIdIn(7L, ids)).thenReturn(members);
        for (int index = 1; index <= 9; index++) {
            stubMetrics(
                    "player" + index,
                    40 + index * 3,
                    0.7 + index * 0.12,
                    5 + index,
                    index % 3 == 0 ? "저격" : index % 3 == 1 ? "돌격" : "특수"
            );
        }

        var response = service.balance(7L, new ClanTeamBalanceRequest(ids, 3, 0));

        assertThat(response.teamSize()).isEqualTo(3);
        assertThat(response.teams()).hasSize(3);
        assertThat(response.teams()).allSatisfy(team -> assertThat(team.members()).hasSize(3));
        assertThat(response.teams()).extracting(team -> team.key())
                .containsExactly("ALPHA", "BRAVO", "CHARLIE");
    }

    private void stubMetrics(String userName, double winRate, double kd, double kills, String primaryClass) {
        when(matchService.getMatchSummary(userName)).thenReturn(summary(winRate, kd, kills));
        when(weaponService.getWeaponStats(userName)).thenReturn(weapon(primaryClass));
    }

    private ClanMember member(Long id, String userName) {
        ClanMember member = new ClanMember();
        member.setId(id);
        member.setUserName(userName);
        member.setClanName("sample-clan");
        return member;
    }

    private MatchSummaryResponseDto summary(double winRate, double kd, double kills) {
        MatchSummaryItemDto item = new MatchSummaryItemDto();
        item.setKey("CLAN");
        item.setMatchCount(20);
        item.setWinRate(winRate);
        item.setAverageKill(kills);
        item.setAverageDeath(kills / kd);
        MatchSummaryResponseDto response = new MatchSummaryResponseDto();
        response.setSummaries(List.of(item));
        return response;
    }

    private WeaponStatsResponseDto weapon(String primaryClass) {
        WeaponStatsResponseDto response = new WeaponStatsResponseDto();
        response.setPrimaryClass(primaryClass);
        response.setCombatType(primaryClass + " 특화형");
        response.setAssaultRate("돌격".equals(primaryClass) ? 60.0 : 10.0);
        response.setSniperRate("저격".equals(primaryClass) ? 60.0 : 10.0);
        response.setSpecialRate(5.0);
        return response;
    }
}
