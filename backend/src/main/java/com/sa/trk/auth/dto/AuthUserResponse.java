package com.sa.trk.auth.dto;

public record AuthUserResponse(
        Long id,
        String email,
        String loginId,
        String suddenNickname,
        String displayName,
        String ouid,
        boolean nicknameVerified,
        boolean admin,
        boolean clanNone) {}
