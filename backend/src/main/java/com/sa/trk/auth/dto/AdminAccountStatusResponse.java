package com.sa.trk.auth.dto;

import java.time.Instant;

public record AdminAccountStatusResponse(
        Long id,
        String displayName,
        String accountStatus,
        String sanctionReason,
        Instant sanctionedAt,
        Long sanctionedById) {
}
