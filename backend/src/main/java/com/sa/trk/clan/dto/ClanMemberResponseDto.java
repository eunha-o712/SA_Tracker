package com.sa.trk.clan.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ClanMemberResponseDto {

    private Long id;
    private String userName;
    private String clanName;
    private LocalDateTime createdAt;
}
