package com.sa.trk.nexon.dto;

import lombok.Data;

@Data
public class UserTierDto {

    private String user_name;
    private String solo_rank_match_tier;
    private Integer solo_rank_match_score;
    private String party_rank_match_tier;
    private Integer party_rank_match_score;
}