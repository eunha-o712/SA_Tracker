package com.sa.trk.auth.dto;

public record AccountStatusUpdateRequest(
        String status,
        String reason) {
}
