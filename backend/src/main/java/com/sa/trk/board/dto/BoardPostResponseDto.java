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
        List<String> imageUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
