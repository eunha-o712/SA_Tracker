package com.sa.trk.ranking.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.entity.RankProgressionType;
import com.sa.trk.ranking.repository.RankProgressionRuleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankProgressionCatalogService {

    private final RankProgressionRuleRepository repository;

    @Transactional(readOnly = true)
    public List<RankProgressionRule> findOrdered(RankProgressionType progressionType) {
        return repository.findByProgressionTypeOrderByDisplayOrderAsc(progressionType);
    }
}
