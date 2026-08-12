package com.sa.trk.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.TierDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserTierDto;
import com.sa.trk.nexon.service.NexonMetaCacheService;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.entity.RankProgressionType;

class RankingServiceTests {

    @Test
    void buildsGradeProgressionFromDatabaseCatalogAndTierProgressionFromMetadata() {
        PlayerService playerService = mock(PlayerService.class);
        NexonMetaCacheService metaService = mock(NexonMetaCacheService.class);
        RankProgressionCatalogService catalogService = mock(RankProgressionCatalogService.class);
        OuidResponseDto ouid = new OuidResponseDto();
        ouid.setOuid("ouid-player");
        UserRankDto rank = new UserRankDto();
        rank.setGrade("Private");
        rank.setGrade_exp(1200);
        rank.setSeason_grade("Season Private");
        rank.setSeason_grade_exp(800);
        UserTierDto tier = new UserTierDto();
        tier.setSolo_rank_match_tier("SILVER I");
        tier.setSolo_rank_match_score(1400);
        tier.setParty_rank_match_tier("GOLD I");
        tier.setParty_rank_match_score(1700);

        when(playerService.getOuid("player")).thenReturn(ouid);
        when(playerService.getUserRank("ouid-player")).thenReturn(rank);
        when(playerService.getUserTier("ouid-player")).thenReturn(tier);
        when(catalogService.findOrdered(RankProgressionType.GRADE)).thenReturn(List.of(
                rule(RankProgressionType.GRADE, 0, "Recruit", 0L, 999L, null, null),
                rule(RankProgressionType.GRADE, 1, "Private", 1000L, 1999L, null, null),
                rule(RankProgressionType.GRADE, 2, "Corporal", 2000L, 2999L, null, null)
        ));
        when(catalogService.findOrdered(RankProgressionType.SEASON_GRADE)).thenReturn(List.of(
                rule(RankProgressionType.SEASON_GRADE, 0, "Season Recruit", 0L, 499L, null, null),
                rule(RankProgressionType.SEASON_GRADE, 1, "Season Private", 500L, null, 101, null),
                rule(RankProgressionType.SEASON_GRADE, 2, "Season Corporal", null, null, 1, 100)
        ));
        when(metaService.findGradeImage("Recruit")).thenReturn("grade-0");
        when(metaService.findGradeImage("Private")).thenReturn("grade-1");
        when(metaService.findGradeImage("Corporal")).thenReturn("grade-2");
        when(metaService.findSeasonGradeImage("Season Recruit")).thenReturn("season-0");
        when(metaService.findSeasonGradeImage("Season Private")).thenReturn("season-1");
        when(metaService.findSeasonGradeImage("Season Corporal")).thenReturn("season-2");
        when(metaService.getTiers()).thenReturn(List.of(
                tier("UNRANK", "tier-0"),
                tier("SILVER I", "tier-1"),
                tier("GOLD I", "tier-2"),
                tier("LEGEND", "tier-3")
        ));

        var response = new RankingService(playerService, metaService, catalogService)
                .getRanking("player");

        assertThat(response.getGradeExp()).isEqualTo(1200);
        assertThat(response.getGradeProgression().getCurrentIndex()).isEqualTo(1);
        assertThat(response.getGradeProgression().getNextName()).isEqualTo("Corporal");
        assertThat(response.getGradeProgression().getCurrentMinimumExperience()).isEqualTo(1000L);
        assertThat(response.getGradeProgression().getCurrentMaximumExperience()).isEqualTo(1999L);
        assertThat(response.getGradeProgression().getNextMinimumExperience()).isEqualTo(2000L);
        assertThat(response.getSeasonGradeProgression().getNextName()).isEqualTo("Season Corporal");
        assertThat(response.getSeasonGradeProgression().getNextWorstRanking()).isEqualTo(100);
        assertThat(response.getSoloTierProgression().getNextName()).isEqualTo("GOLD I");
        assertThat(response.getPartyTierProgression().getNextName()).isEqualTo("LEGEND");
    }

    private RankProgressionRule rule(
            RankProgressionType type,
            int order,
            String name,
            Long minimumExperience,
            Long maximumExperience,
            Integer bestRanking,
            Integer worstRanking
    ) {
        RankProgressionRule rule = new RankProgressionRule();
        rule.setProgressionType(type);
        rule.setDisplayOrder(order);
        rule.setRankName(name);
        rule.setMinimumExperience(minimumExperience);
        rule.setMaximumExperience(maximumExperience);
        rule.setBestRanking(bestRanking);
        rule.setWorstRanking(worstRanking);
        return rule;
    }

    private TierDto tier(String name, String image) {
        TierDto tier = new TierDto();
        tier.setTier(name);
        tier.setTier_image(image);
        return tier;
    }
}
