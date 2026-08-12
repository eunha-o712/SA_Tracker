package com.sa.trk.ranking.service;

import java.util.List;
import java.util.function.Function;

import org.springframework.stereotype.Service;

import com.sa.trk.common.dto.ImagesDto;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.GradeDto;
import com.sa.trk.nexon.dto.SeasonGradeDto;
import com.sa.trk.nexon.dto.TierDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserTierDto;
import com.sa.trk.nexon.service.NexonMetaCacheService;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.ranking.dto.RankingResponseDto;
import com.sa.trk.ranking.dto.RankingProgressionDto;
import com.sa.trk.ranking.entity.RankProgressionRule;
import com.sa.trk.ranking.entity.RankProgressionType;

@Service
public class RankingService {

    private final PlayerService playerService;
    private final NexonMetaCacheService nexonMetaCacheService;
    private final RankProgressionCatalogService rankProgressionCatalogService;

    public RankingService(
            PlayerService playerService,
            NexonMetaCacheService nexonMetaCacheService,
            RankProgressionCatalogService rankProgressionCatalogService
    ) {
        this.playerService = playerService;
        this.nexonMetaCacheService = nexonMetaCacheService;
        this.rankProgressionCatalogService = rankProgressionCatalogService;
    }

    public RankingResponseDto getRanking(String userName) {
        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        UserRankDto rankInfo = playerService.getUserRank(ouid);
        UserTierDto tierInfo = playerService.getUserTier(ouid);

        RankingResponseDto responseDto = new RankingResponseDto();
        responseDto.setUserName(userName);
        responseDto.setGrade(rankInfo.getGrade());
        responseDto.setGradeExp(rankInfo.getGrade_exp());
        responseDto.setGradeRanking(rankInfo.getGrade_ranking());
        responseDto.setSeasonGrade(rankInfo.getSeason_grade());
        responseDto.setSeasonGradeExp(rankInfo.getSeason_grade_exp());
        responseDto.setSeasonGradeRanking(rankInfo.getSeason_grade_ranking());
        responseDto.setSoloRankMatchTier(tierInfo.getSolo_rank_match_tier());
        responseDto.setSoloRankMatchScore(tierInfo.getSolo_rank_match_score());
        responseDto.setPartyRankMatchTier(tierInfo.getParty_rank_match_tier());
        responseDto.setPartyRankMatchScore(tierInfo.getParty_rank_match_score());

        ImagesDto images = new ImagesDto();
        images.setGradeImage(nexonMetaCacheService.findGradeImage(rankInfo.getGrade()));
        images.setSeasonGradeImage(
                nexonMetaCacheService.findSeasonGradeImage(rankInfo.getSeason_grade())
        );
        images.setSoloTierImage(
                nexonMetaCacheService.findTierImage(tierInfo.getSolo_rank_match_tier())
        );
        images.setPartyTierImage(
                nexonMetaCacheService.findTierImage(tierInfo.getParty_rank_match_tier())
        );
        responseDto.setImages(images);

        responseDto.setGradeProgression(buildGradeProgression(rankInfo.getGrade()));
        responseDto.setSeasonGradeProgression(buildSeasonGradeProgression(rankInfo.getSeason_grade()));
        responseDto.setSoloTierProgression(buildProgression(
                nexonMetaCacheService.getTiers(),
                TierDto::getTier,
                TierDto::getTier_image,
                tierInfo.getSolo_rank_match_tier()
        ));
        responseDto.setPartyTierProgression(buildProgression(
                nexonMetaCacheService.getTiers(),
                TierDto::getTier,
                TierDto::getTier_image,
                tierInfo.getParty_rank_match_tier()
        ));

        return responseDto;
    }

    private RankingProgressionDto buildGradeProgression(String currentName) {
        List<RankProgressionRule> rules = rankProgressionCatalogService.findOrdered(
                RankProgressionType.GRADE
        );
        if (!rules.isEmpty()) {
            return buildRuleProgression(rules, currentName, nexonMetaCacheService::findGradeImage);
        }
        return buildProgression(
                nexonMetaCacheService.getGrades(),
                GradeDto::getGrade,
                GradeDto::getGrade_image,
                currentName
        );
    }

    private RankingProgressionDto buildSeasonGradeProgression(String currentName) {
        List<RankProgressionRule> rules = rankProgressionCatalogService.findOrdered(
                RankProgressionType.SEASON_GRADE
        );
        if (!rules.isEmpty()) {
            return buildRuleProgression(
                    rules,
                    currentName,
                    nexonMetaCacheService::findSeasonGradeImage
            );
        }
        return buildProgression(
                nexonMetaCacheService.getSeasonGrades(),
                SeasonGradeDto::getSeason_grade,
                SeasonGradeDto::getSeason_grade_image,
                currentName
        );
    }

    private RankingProgressionDto buildRuleProgression(
            List<RankProgressionRule> rules,
            String currentName,
            Function<String, String> imageFinder
    ) {
        RankingProgressionDto progression = buildProgression(
                rules,
                RankProgressionRule::getRankName,
                rule -> imageFinder.apply(rule.getRankName()),
                currentName
        );

        Integer currentIndex = progression.getCurrentIndex();
        if (currentIndex == null || currentIndex < 0 || currentIndex >= rules.size()) {
            return progression;
        }

        RankProgressionRule current = rules.get(currentIndex);
        progression.setCurrentMinimumExperience(current.getMinimumExperience());
        progression.setCurrentMaximumExperience(current.getMaximumExperience());

        if (currentIndex + 1 < rules.size()) {
            RankProgressionRule next = rules.get(currentIndex + 1);
            progression.setNextMinimumExperience(next.getMinimumExperience());
            progression.setNextBestRanking(next.getBestRanking());
            progression.setNextWorstRanking(next.getWorstRanking());
        }
        return progression;
    }

    private <T> RankingProgressionDto buildProgression(
            List<T> entries,
            Function<T, String> nameExtractor,
            Function<T, String> imageExtractor,
            String currentName) {
        RankingProgressionDto progression = new RankingProgressionDto();
        progression.setTotalCount(entries.size());
        progression.setCurrentName(currentName);

        if (entries.isEmpty()) {
            progression.setCurrentIndex(-1);
            return progression;
        }

        T minimum = entries.get(0);
        T maximum = entries.get(entries.size() - 1);
        progression.setMinimumName(nameExtractor.apply(minimum));
        progression.setMinimumImage(imageExtractor.apply(minimum));
        progression.setMaximumName(nameExtractor.apply(maximum));
        progression.setMaximumImage(imageExtractor.apply(maximum));

        int currentIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            if (sameText(nameExtractor.apply(entries.get(index)), currentName)) {
                currentIndex = index;
                break;
            }
        }
        progression.setCurrentIndex(currentIndex);

        if (currentIndex < 0) {
            return progression;
        }

        T current = entries.get(currentIndex);
        progression.setCurrentName(nameExtractor.apply(current));
        progression.setCurrentImage(imageExtractor.apply(current));

        if (currentIndex + 1 < entries.size()) {
            T next = entries.get(currentIndex + 1);
            progression.setNextName(nameExtractor.apply(next));
            progression.setNextImage(imageExtractor.apply(next));
        }
        return progression;
    }

    private boolean sameText(String left, String right) {
        return left != null && right != null && left.trim().equalsIgnoreCase(right.trim());
    }
}
