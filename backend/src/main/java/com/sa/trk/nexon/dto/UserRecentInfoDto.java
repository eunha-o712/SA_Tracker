package com.sa.trk.nexon.dto;

import lombok.Data;

@Data
public class UserRecentInfoDto {

    private String user_name;
    private Double recent_win_rate;
    private Double recent_kill_death_rate;
    private Double recent_assault_rate;
    private Double recent_sniper_rate;
    private Double recent_special_rate;
}