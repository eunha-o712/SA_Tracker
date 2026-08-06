package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sa.trk.clan.entity.ClanMember;
import com.sa.trk.clan.repository.ClanMemberRepository;

class ClanDashboardServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private ClanMemberMetricsService clanMemberMetricsService;

    private ClanDashboardService clanDashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clanDashboardService = new ClanDashboardService(clanMemberRepository, clanMemberMetricsService);
    }

    @Test
    void aggregatesAvailableMembersAndIsolatesMemberFailures() {
        ClanMember alpha = member(1L, "alpha", "sample-clan");
        ClanMember beta = member(2L, "beta", "sample-clan");
        setStats(alpha, 10, 6, 1, 3, 60.0, 2.0, true);
        setStats(beta, 0, 0, 0, 0, 0, 0, false);
        when(clanMemberRepository.findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(7L, "sample-clan"))
                .thenReturn(List.of(alpha, beta));

        var dashboard = clanDashboardService.getDashboard(7L, " sample-clan ");

        assertThat(dashboard.getClanName()).isEqualTo("sample-clan");
        assertThat(dashboard.getMemberCount()).isEqualTo(2);
        assertThat(dashboard.getAnalyzedMemberCount()).isEqualTo(1);
        assertThat(dashboard.getTotalMatchCount()).isEqualTo(10);
        assertThat(dashboard.getAverageWinRate()).isEqualTo(60.0);
        assertThat(dashboard.getAverageKillDeathRatio()).isEqualTo(2.0);
        assertThat(dashboard.getMembers()).extracting("userName").containsExactly("alpha", "beta");
        assertThat(dashboard.getMembers().get(0).getAvailable()).isTrue();
        assertThat(dashboard.getMembers().get(1).getAvailable()).isFalse();
    }

    @Test
    void rejectsABlankClanNameBeforeLoadingMembers() {
        assertThatThrownBy(() -> clanDashboardService.getDashboard(7L, "  "))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(clanMemberRepository, clanMemberMetricsService);
    }

    @Test
    void refreshesEveryRosterMemberBeforeReturningDashboard() {
        ClanMember alpha = member(1L, "alpha", "sample-clan");
        ClanMember beta = member(2L, "beta", "sample-clan");
        setStats(alpha, 10, 6, 1, 3, 60, 2, true);
        setStats(beta, 8, 4, 1, 3, 50, 1.2, true);
        when(clanMemberRepository.findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(7L, "sample-clan"))
                .thenReturn(List.of(alpha, beta));
        when(clanMemberMetricsService.refreshMember(alpha)).thenReturn(alpha);
        when(clanMemberMetricsService.refreshMember(beta)).thenReturn(beta);

        var dashboard = clanDashboardService.refreshDashboard(7L, "sample-clan");

        verify(clanMemberMetricsService).refreshMember(alpha);
        verify(clanMemberMetricsService).refreshMember(beta);
        assertThat(dashboard.getAnalyzedMemberCount()).isEqualTo(2);
    }

    private ClanMember member(Long id, String userName, String clanName) {
        ClanMember member = new ClanMember();
        member.setId(id);
        member.setUserName(userName);
        member.setClanName(clanName);
        return member;
    }

    private void setStats(
            ClanMember member,
            int matchCount,
            int winCount,
            int drawCount,
            int loseCount,
            double winRate,
            double killDeathRatio,
            boolean available) {
        member.setStatsMatchCount(matchCount);
        member.setStatsWinCount(winCount);
        member.setStatsDrawCount(drawCount);
        member.setStatsLoseCount(loseCount);
        member.setStatsWinRate(winRate);
        member.setStatsKillDeathRatio(killDeathRatio);
        member.setStatsAvailable(available);
        member.setStatsUpdatedAt(LocalDateTime.of(2026, 7, 31, 10, 0));
    }
}
