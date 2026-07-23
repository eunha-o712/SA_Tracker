package com.sa.trk.board.dto;

public record BoardPostCreateRequest(
        String type,
        String title,
        String content,
        String supportCategory,
        String suddenNickname) {

    public BoardPostCreateRequest(String type, String title, String content) {
        this(type, title, content, null, null);
    }
}
