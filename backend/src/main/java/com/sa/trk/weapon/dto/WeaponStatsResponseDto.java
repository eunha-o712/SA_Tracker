package com.sa.trk.weapon.dto;

import lombok.Data;

@Data
public class WeaponStatsResponseDto {

    private String userName;
    private Double assaultRate;
    private Double sniperRate;
    private Double specialRate;
}