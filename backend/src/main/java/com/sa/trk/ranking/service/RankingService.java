package com.sa.trk.ranking.service;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserTierDto;
import com.sa.trk.player.service.PlayerService;
import com.sa.trk.ranking.dto.RankingResponseDto;

@Service
public class RankingService {

    private final PlayerService playerService;

    public RankingService(PlayerService playerService) {
        this.playerService = playerService;
    }

    public RankingResponseDto getRanking(String userName) {
        OuidResponseDto ouidResponse = playerService.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        UserRankDto rankInfo = playerService.getUserRank(ouid);
        UserTierDto tierInfo = playerService.getUserTier(ouid);

        RankingResponseDto responseDto = new RankingResponseDto();
        responseDto.setUserName(userName);
        responseDto.setGrade(rankInfo.getGrade());
        responseDto.setGradeRanking(rankInfo.getGrade_ranking());
        responseDto.setSeasonGrade(rankInfo.getSeason_grade());
        responseDto.setSeasonGradeRanking(rankInfo.getSeason_grade_ranking());
        responseDto.setSoloRankMatchTier(tierInfo.getSolo_rank_match_tier());
        responseDto.setSoloRankMatchScore(tierInfo.getSolo_rank_match_score());
        responseDto.setPartyRankMatchTier(tierInfo.getParty_rank_match_tier());
        responseDto.setPartyRankMatchScore(tierInfo.getParty_rank_match_score());

        return responseDto;
    }
}