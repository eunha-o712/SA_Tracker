package com.sa.trk.board.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.board.dto.BoardPostCreateRequest;
import com.sa.trk.board.dto.BoardPostResponseDto;
import com.sa.trk.board.service.BoardPostService;
import com.sa.trk.board.service.BoardImageStorageService;

@RestController
@RequestMapping("/api/board/posts")
public class BoardPostController {

    private final BoardPostService boardPostService;
    private final BoardImageStorageService boardImageStorageService;
    private final AuthService authService;

    public BoardPostController(
            BoardPostService boardPostService,
            BoardImageStorageService boardImageStorageService,
            AuthService authService) {
        this.boardPostService = boardPostService;
        this.boardImageStorageService = boardImageStorageService;
        this.authService = authService;
    }

    @GetMapping
    public List<BoardPostResponseDto> getPosts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("type") String type) {
        AuthUserResponse viewer = "SUPPORT".equalsIgnoreCase(type == null ? "" : type.trim())
                ? authService.currentUser(bearerToken(authorization))
                : null;
        return boardPostService.getPosts(type, viewer);
    }

    @GetMapping("/{id}")
    public BoardPostResponseDto getPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id) {
        String token = bearerToken(authorization);
        AuthUserResponse viewer = token == null ? null : authService.currentUser(token);
        return boardPostService.getPost(id, viewer);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BoardPostResponseDto createPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestBody BoardPostCreateRequest request) {
        AuthUserResponse author = authService.currentUser(bearerToken(authorization));
        return boardPostService.createPost(request, author);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BoardPostResponseDto createPostWithImages(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "supportCategory", required = false) String supportCategory,
            @RequestParam(value = "suddenNickname", required = false) String suddenNickname,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        AuthUserResponse author = authService.currentUser(bearerToken(authorization));
        List<String> imageUrls = boardImageStorageService.store(images);
        try {
            return boardPostService.createPost(
                    new BoardPostCreateRequest(type, title, content, supportCategory, suddenNickname),
                    author,
                    imageUrls
            );
        } catch (RuntimeException exception) {
            boardImageStorageService.delete(imageUrls);
            throw exception;
        }
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
