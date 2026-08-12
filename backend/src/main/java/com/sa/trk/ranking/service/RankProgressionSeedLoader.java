package com.sa.trk.ranking.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.entity.RankProgressionType;

@Component
public class RankProgressionSeedLoader {

    private static final String RESOURCE_PATH = "rank-progression-rules.csv";

    public List<RankProgressionRule> load() {
        ClassPathResource resource = new ClassPathResource(RESOURCE_PATH);
        List<RankProgressionRule> rules = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                rules.add(parse(line));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("계급 기준 데이터를 읽을 수 없습니다.", exception);
        }

        validate(rules);
        return rules;
    }

    private RankProgressionRule parse(String line) {
        String[] values = line.split(",", -1);
        if (values.length != 8) {
            throw new IllegalStateException("잘못된 계급 기준 행입니다: " + line);
        }

        RankProgressionRule rule = new RankProgressionRule();
        rule.setProgressionType(RankProgressionType.valueOf(values[0]));
        rule.setDisplayOrder(Integer.parseInt(values[1]));
        rule.setRankGroup(values[2]);
        rule.setRankName(values[3]);
        rule.setMinimumExperience(parseLong(values[4]));
        rule.setMaximumExperience(parseLong(values[5]));
        rule.setBestRanking(parseInteger(values[6]));
        rule.setWorstRanking(parseInteger(values[7]));
        return rule;
    }

    private void validate(List<RankProgressionRule> rules) {
        validateType(rules, RankProgressionType.GRADE, 60);
        validateType(rules, RankProgressionType.SEASON_GRADE, 56);
    }

    private void validateType(
            List<RankProgressionRule> rules,
            RankProgressionType type,
            int expectedCount
    ) {
        List<RankProgressionRule> typedRules = rules.stream()
                .filter(rule -> rule.getProgressionType() == type)
                .sorted((left, right) -> Integer.compare(left.getDisplayOrder(), right.getDisplayOrder()))
                .toList();

        if (typedRules.size() != expectedCount) {
            throw new IllegalStateException(type + " 계급 수가 올바르지 않습니다: " + typedRules.size());
        }
        for (int index = 0; index < typedRules.size(); index++) {
            if (typedRules.get(index).getDisplayOrder() != index) {
                throw new IllegalStateException(type + " 계급 순서가 연속적이지 않습니다: " + index);
            }
        }
    }

    private Long parseLong(String value) {
        return value.isBlank() ? null : Long.valueOf(value);
    }

    private Integer parseInteger(String value) {
        return value.isBlank() ? null : Integer.valueOf(value);
    }
}
