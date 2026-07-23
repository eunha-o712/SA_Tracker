package com.sa.trk.board.dto;

public record OuidDisputeResolutionRequest(
        String resolution,
        String accountAction,
        Boolean verifyClaimant,
        String response,
        String reason) {
}
