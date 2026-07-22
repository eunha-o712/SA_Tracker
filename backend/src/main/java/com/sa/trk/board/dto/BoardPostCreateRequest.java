package com.sa.trk.board.dto;

public record BoardPostCreateRequest(
        String type,
        String title,
        String content) {
}
