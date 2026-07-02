package com.sa.trk.player.service;

import org.springframework.stereotype.Service;

import com.sa.trk.common.dto.ImagesDto;
import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.nexon.dto.UserTierDto;
import com.sa.trk.nexon.service.NexonMetaCacheService;
import com.sa.trk.player.dto.PlayerResponseDto;

@Service
public class PlayerService {

    private final NexonApiClient nexonApiClient;
    private final NexonMetaCacheService nexonMetaCacheService;

    public PlayerService(
            NexonApiClient nexonApiClient,
            NexonMetaCacheService nexonMetaCacheService
    ) {
        this.nexonApiClient = nexonApiClient;
        this.nexonMetaCacheService = nexonMetaCacheService;
    }

    public PlayerResponseDto getPlayer(String userName) {
        OuidResponseDto ouidResponse = nexonApiClient.getOuid(userName);
        String ouid = ouidResponse.getOuid();

        UserBasicDto basic = nexonApiClient.getUserBasic(ouid);
        UserRankDto rank = nexonApiClient.getUserRank(ouid);
        UserTierDto tier = nexonApiClient.getUserTier(ouid);
        UserRecentInfoDto recent = nexonApiClient.getUserRecentInfo(ouid);

        ImagesDto images = new ImagesDto();
        images.setGradeImage(
                nexonMetaCacheService.findGradeImage(rank.getGrade())
        );
        images.setSeasonGradeImage(
                nexonMetaCacheService.findSeasonGradeImage(rank.getSeason_grade())
        );
        images.setSoloTierImage(
                nexonMetaCacheService.findTierImage(tier.getSolo_rank_match_tier())
        );
        images.setPartyTierImage(
                nexonMetaCacheService.findTierImage(tier.getParty_rank_match_tier())
        );
        images.setLogoImage(
                nexonMetaCacheService.getLogoImage()
        );

        PlayerResponseDto response = new PlayerResponseDto();
        response.setUserName(basic.getUser_name());
        response.setOuid(ouid);
        response.setBasic(basic);
        response.setRank(rank);
        response.setTier(tier);
        response.setRecent(recent);
        response.setImages(images);

        return response;
    }

    public OuidResponseDto getOuid(String userName) {
        return nexonApiClient.getOuid(userName);
    }

    public UserBasicDto getUserBasic(String ouid) {
        return nexonApiClient.getUserBasic(ouid);
    }

    public UserRankDto getUserRank(String ouid) {
        return nexonApiClient.getUserRank(ouid);
    }

    public UserTierDto getUserTier(String ouid) {
        return nexonApiClient.getUserTier(ouid);
    }

    public UserRecentInfoDto getUserRecentInfo(String ouid) {
        return nexonApiClient.getUserRecentInfo(ouid);
    }
}