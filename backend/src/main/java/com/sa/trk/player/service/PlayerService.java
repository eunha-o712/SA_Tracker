package com.sa.trk.player.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sa.trk.common.dto.ImagesDto;
import com.sa.trk.config.ClanTestProperties;
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

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final NexonApiClient nexonApiClient;
    private final NexonMetaCacheService nexonMetaCacheService;
    private final ClanTestProperties clanTestProperties;

    public PlayerService(
            NexonApiClient nexonApiClient,
            NexonMetaCacheService nexonMetaCacheService,
            ClanTestProperties clanTestProperties
    ) {
        this.nexonApiClient = nexonApiClient;
        this.nexonMetaCacheService = nexonMetaCacheService;
        this.clanTestProperties = clanTestProperties;
        if (clanTestProperties.isEnabled()) {
            log.warn(
                    "Local clan test override is enabled: {} -> {}",
                    clanTestProperties.getUserName(),
                    clanTestProperties.getClanName()
            );
        }
    }

    public PlayerResponseDto getPlayer(String userName) {
        OuidResponseDto ouidResponse = nexonApiClient.getOuid(userName);
        return getPlayerByOuid(ouidResponse.getOuid());
    }

    public PlayerResponseDto getPlayerByOuid(String ouid) {

        UserBasicDto basic = getUserBasic(ouid);
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
        UserBasicDto basic = nexonApiClient.getUserBasic(ouid);
        if (shouldOverrideClan(basic)) {
            basic.setClan_name(clanTestProperties.getClanName().trim());
        }
        return basic;
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

    private boolean shouldOverrideClan(UserBasicDto basic) {
        if (!clanTestProperties.isEnabled() || basic == null) {
            return false;
        }

        String configuredUserName = normalize(clanTestProperties.getUserName());
        String configuredClanName = normalize(clanTestProperties.getClanName());
        String actualUserName = normalize(basic.getUser_name());
        return !configuredUserName.isEmpty()
                && !configuredClanName.isEmpty()
                && configuredUserName.equalsIgnoreCase(actualUserName);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
