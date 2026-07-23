package com.sa.trk.board.dto;

import java.time.Instant;

public record SupportActionLogResponse(
        Long id,
        String action,
        String note,
        Long actorId,
        String actorName,
        Instant createdAt) {
}
