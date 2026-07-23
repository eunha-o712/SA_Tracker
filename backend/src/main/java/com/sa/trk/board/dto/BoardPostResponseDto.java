package com.sa.trk.board.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BoardPostResponseDto(
        Long id,
        String type,
        String title,
        String content,
        Long authorId,
        String authorName,
        long viewCount,
        boolean notice,
        boolean privatePost,
        boolean authorLinked,
        boolean authorVerified,
        String authorAccountStatus,
        String supportCategory,
        String supportStatus,
        String claimedSuddenNickname,
        Long claimedOwnerId,
        String claimedOwnerName,
        String claimedOwnerAccountStatus,
        String adminResponse,
        String resolutionAction,
        String accountSanctionAction,
        boolean claimantVerified,
        Long handledById,
        String handledByName,
        java.time.Instant handledAt,
        List<SupportActionLogResponse> actionLogs,
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
