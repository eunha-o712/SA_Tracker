package com.sa.trk.ranking.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.entity.RankProgressionType;

public interface RankProgressionRuleRepository extends JpaRepository<RankProgressionRule, Long> {

    List<RankProgressionRule> findByProgressionTypeOrderByDisplayOrderAsc(
            RankProgressionType progressionType
    );
}
