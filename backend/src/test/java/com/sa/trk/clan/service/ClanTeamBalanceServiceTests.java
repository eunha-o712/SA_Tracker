package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.clan.dto.ClanTeamBalanceRequest;
import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;

class ClanTeamBalanceServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    private ClanTeamBalanceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ClanTeamBalanceService(clanMemberRepository);
    }

    @Test
    void balancesPowerAndDistributesSnipersAcrossTeams() {
        List<ClanMember> members = List.of(
                member(1L, "alpha", 70, 2.0, 14, "저격"),
                member(2L, "bravo", 50, 1.0, 8, "저격"),
                member(3L, "charlie", 65, 1.8, 13, "돌격"),
                member(4L, "delta", 45, 0.9, 7, "돌격")
        );
        when(clanMemberRepository.findAllByOwnerIdAndIdIn(7L, List.of(1L, 2L, 3L, 4L)))
                .thenReturn(members);

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
                .thenReturn(List.of(
                        member(1L, "alpha", 50, 1, 8, "돌격"),
                        member(2L, "bravo", 50, 1, 8, "돌격"),
                        member(3L, "charlie", 50, 1, 8, "돌격")
                ));

        assertThatThrownBy(() -> service.balance(
                7L,
                new ClanTeamBalanceRequest(List.of(1L, 2L, 3L, 4L), 2, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("본인 클랜 로스터");
    }

    @Test
    void requiresPlayerCountToBeDivisibleByTeamSize() {
        assertThatThrownBy(() -> service.balance(
                7L,
                new ClanTeamBalanceRequest(List.of(1L, 2L, 3L, 4L, 5L), 2, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정확히 나누어져야");
    }

    @Test
    void dividesNinePlayersIntoThreeTeamsOfThree() {
        List<ClanMember> members = List.of(
                member(1L, "player1", 43, .82, 6, "돌격"),
                member(2L, "player2", 46, .94, 7, "특수"),
                member(3L, "player3", 49, 1.06, 8, "저격"),
                member(4L, "player4", 52, 1.18, 9, "돌격"),
                member(5L, "player5", 55, 1.30, 10, "특수"),
                member(6L, "player6", 58, 1.42, 11, "저격"),
                member(7L, "player7", 61, 1.54, 12, "돌격"),
                member(8L, "player8", 64, 1.66, 13, "특수"),
                member(9L, "player9", 67, 1.78, 14, "저격")
        );
        List<Long> ids = members.stream().map(ClanMember::getId).toList();
        when(clanMemberRepository.findAllByOwnerIdAndIdIn(7L, ids)).thenReturn(members);
        var response = service.balance(7L, new ClanTeamBalanceRequest(ids, 3, 0));

        assertThat(response.teamSize()).isEqualTo(3);
        assertThat(response.teams()).hasSize(3);
        assertThat(response.teams()).allSatisfy(team -> assertThat(team.members()).hasSize(3));
        assertThat(response.teams()).extracting(team -> team.key())
                .containsExactly("ALPHA", "BRAVO", "CHARLIE");
    }

    private ClanMember member(
            Long id,
            String userName,
            double winRate,
            double kd,
            double kills,
            String primaryClass) {
        ClanMember member = new ClanMember();
        member.setId(id);
        member.setUserName(userName);
        member.setClanName("sample-clan");
        member.setStatsAvailable(true);
        member.setStatsMatchCount(20);
        member.setStatsWinRate(winRate);
        member.setStatsKillDeathRatio(kd);
        member.setStatsAverageKill(kills);
        member.setStatsPrimaryClass(primaryClass);
        member.setStatsCombatType(primaryClass + " 특화형");
        member.setStatsPowerScore(powerScore(winRate, kd, kills));
        return member;
    }

    private double powerScore(double winRate, double kd, double kills) {
        double winScore = Math.max(0, Math.min(100, winRate));
        double kdScore = Math.max(0, Math.min(3, kd)) / 3.0 * 100.0;
        double killScore = Math.max(0, Math.min(20, kills)) / 20.0 * 100.0;
        return Math.round((winScore * .45 + kdScore * .35 + killScore * .20) * 10.0) / 10.0;
    }
}
