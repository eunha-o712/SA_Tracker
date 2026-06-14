package com.sa.trk.player.service;

import org.springframework.stereotype.Service;

import com.sa.trk.nexon.client.NexonApiClient;
import com.sa.trk.nexon.dto.OuidResponseDto;
import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.nexon.dto.UserTierDto;

@Service
public class PlayerService {

    private final NexonApiClient nexonApiClient;

    public PlayerService(NexonApiClient nexonApiClient) {
        this.nexonApiClient = nexonApiClient;
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