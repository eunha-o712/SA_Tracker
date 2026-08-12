package com.sa.trk.ranking.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.repository.RankProgressionRuleRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RankProgressionDataInitializer implements ApplicationRunner {

    private final RankProgressionRuleRepository repository;
    private final RankProgressionSeedLoader seedLoader;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<RankProgressionRule> expected = seedLoader.load();
        List<RankProgressionRule> existing = repository.findAll().stream()
                .sorted(ruleComparator())
                .toList();

        if (sameRules(existing, expected)) {
            return;
        }

        repository.deleteAllInBatch();
        repository.saveAll(expected);
    }

    private boolean sameRules(List<RankProgressionRule> existing, List<RankProgressionRule> expected) {
        if (existing.size() != expected.size()) {
            return false;
        }

        List<RankProgressionRule> sortedExpected = expected.stream()
                .sorted(ruleComparator())
                .toList();
        for (int index = 0; index < existing.size(); index++) {
            if (!sameRule(existing.get(index), sortedExpected.get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean sameRule(RankProgressionRule left, RankProgressionRule right) {
        return left.getProgressionType() == right.getProgressionType()
                && left.getDisplayOrder() == right.getDisplayOrder()
                && java.util.Objects.equals(left.getRankGroup(), right.getRankGroup())
                && java.util.Objects.equals(left.getRankName(), right.getRankName())
                && java.util.Objects.equals(left.getMinimumExperience(), right.getMinimumExperience())
                && java.util.Objects.equals(left.getMaximumExperience(), right.getMaximumExperience())
                && java.util.Objects.equals(left.getBestRanking(), right.getBestRanking())
                && java.util.Objects.equals(left.getWorstRanking(), right.getWorstRanking());
    }

    private Comparator<RankProgressionRule> ruleComparator() {
        return Comparator.comparing(RankProgressionRule::getProgressionType)
                .thenComparingInt(RankProgressionRule::getDisplayOrder);
    }
}
