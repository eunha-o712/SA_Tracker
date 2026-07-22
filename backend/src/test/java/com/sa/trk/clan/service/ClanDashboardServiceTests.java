package com.sa.trk.clan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
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

class ClanDashboardServiceTests {

    @Mock
    private ClanMemberRepository clanMemberRepository;

    @Mock
    private MatchService matchService;

    private ClanDashboardService clanDashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clanDashboardService = new ClanDashboardService(clanMemberRepository, matchService);
    }

    @Test
    void aggregatesAvailableMembersAndIsolatesMemberFailures() {
        ClanMember alpha = member(1L, "alpha", "sample-clan");
        ClanMember beta = member(2L, "beta", "sample-clan");
        when(clanMemberRepository.findAllByOwnerIdAndClanNameIgnoreCaseOrderByUserNameAsc(7L, "sample-clan"))
                .thenReturn(List.of(alpha, beta));
        when(matchService.getMatchSummary("alpha")).thenReturn(summary(10, 6, 1, 3, 60.0, 8.0, 4.0));
        when(matchService.getMatchSummary("beta")).thenThrow(new IllegalStateException("temporary failure"));

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

        verifyNoInteractions(clanMemberRepository, matchService);
    }

    private ClanMember member(Long id, String userName, String clanName) {
        ClanMember member = new ClanMember();
        member.setId(id);
        member.setUserName(userName);
        member.setClanName(clanName);
        return member;
    }

    private MatchSummaryResponseDto summary(
            int matchCount,
            int winCount,
            int drawCount,
            int loseCount,
            double winRate,
            double averageKill,
            double averageDeath) {
        MatchSummaryItemDto clan = new MatchSummaryItemDto();
        clan.setKey("CLAN");
        clan.setMatchCount(matchCount);
        clan.setWinCount(winCount);
        clan.setDrawCount(drawCount);
        clan.setLoseCount(loseCount);
        clan.setWinRate(winRate);
        clan.setAverageKill(averageKill);
        clan.setAverageDeath(averageDeath);

        MatchSummaryResponseDto response = new MatchSummaryResponseDto();
        response.setSummaries(List.of(clan));
        return response;
    }
}
