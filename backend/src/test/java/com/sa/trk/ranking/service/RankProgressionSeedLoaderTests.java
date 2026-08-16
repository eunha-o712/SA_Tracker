package com.sa.trk.ranking.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sa.trk.ranking.entity.RankProgressionType;

class RankProgressionSeedLoaderTests {

    private final RankProgressionSeedLoader loader = new RankProgressionSeedLoader();

    @Test
    void loadsAllIntegratedAndSeasonGradeRulesWithoutMissingOrders() {
        var rules = loader.load();
        var grades = rules.stream()
                .filter(rule -> rule.getProgressionType() == RankProgressionType.GRADE)
                .toList();
        var seasonGrades = rules.stream()
                .filter(rule -> rule.getProgressionType() == RankProgressionType.SEASON_GRADE)
                .toList();

        assertThat(grades).hasSize(60);
        assertThat(grades.getFirst().getRankName()).isEqualTo("훈련병");
        assertThat(grades.getLast().getRankName()).isEqualTo("대원수");
        assertThat(grades.get(53).getMinimumExperience()).isEqualTo(25_000_000L);
        assertThat(grades.get(59).getBestRanking()).isEqualTo(1);

        assertThat(seasonGrades).hasSize(56);
        assertThat(seasonGrades.getFirst().getRankName()).isEqualTo("특등이병");
        assertThat(seasonGrades.get(12).getRankName()).isEqualTo("특정중사 4호봉");
        assertThat(seasonGrades.getLast().getRankName()).isEqualTo("총사령관");
        assertThat(seasonGrades.get(52).getMinimumExperience()).isEqualTo(19_556_052L);
        assertThat(seasonGrades.get(55).getWorstRanking()).isEqualTo(10);
    }
}
