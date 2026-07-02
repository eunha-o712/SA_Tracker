package com.sa.trk.player.dto;

import com.sa.trk.nexon.dto.UserBasicDto;
import com.sa.trk.nexon.dto.UserRankDto;
import com.sa.trk.nexon.dto.UserRecentInfoDto;
import com.sa.trk.nexon.dto.UserTierDto;
import com.sa.trk.common.dto.ImagesDto;

import lombok.Data;

@Data
public class PlayerResponseDto {

    private String userName;
    private String ouid;
    private UserBasicDto basic;
    private UserRankDto rank;
    private UserTierDto tier;
    private UserRecentInfoDto recent;
    private ImagesDto images;
}