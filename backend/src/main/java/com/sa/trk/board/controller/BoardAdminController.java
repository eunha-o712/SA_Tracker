package com.sa.trk.board.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import com.sa.trk.auth.dto.AccountStatusUpdateRequest;
import com.sa.trk.auth.service.AuthService;
import com.sa.trk.board.dto.BoardPostCreateRequest;
import com.sa.trk.board.dto.BoardPostResponseDto;
import com.sa.trk.board.dto.OuidDisputeResolutionRequest;
import com.sa.trk.board.dto.SupportAdminUpdateRequest;
import com.sa.trk.board.service.BoardImageStorageService;
import com.sa.trk.board.service.BoardPostService;

@RestController
@RequestMapping("/api/admin/board/posts")
public class BoardAdminController {

    private final BoardPostService boardPostService;
    private final BoardImageStorageService boardImageStorageService;
    private final AuthService authService;

    public BoardAdminController(
            BoardPostService boardPostService,
            BoardImageStorageService boardImageStorageService,
            AuthService authService) {
        this.boardPostService = boardPostService;
        this.boardImageStorageService = boardImageStorageService;
        this.authService = authService;
    }

    @GetMapping
    public List<BoardPostResponseDto> getAllPosts(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.requireAdmin(bearerToken(authorization));
        return boardPostService.getAllPosts();
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public BoardPostResponseDto createPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam("type") String type,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "supportCategory", required = false) String supportCategory,
            @RequestParam(value = "suddenNickname", required = false) String suddenNickname,
            @RequestParam(value = "notice", defaultValue = "false") boolean notice,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {
        AuthUserResponse admin = authService.requireAdmin(bearerToken(authorization));
        List<String> imageUrls = boardImageStorageService.store(images);
        try {
            return boardPostService.createPost(
                    new BoardPostCreateRequest(type, title, content, supportCategory, suddenNickname),
                    admin,
                    imageUrls,
                    notice
            );
        } catch (RuntimeException exception) {
            boardImageStorageService.delete(imageUrls);
            throw exception;
        }
    }

    @PatchMapping("/{id}/support")
    public BoardPostResponseDto updateSupportPost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id,
            @RequestBody SupportAdminUpdateRequest request) {
        AuthUserResponse admin = authService.requireAdmin(bearerToken(authorization));
        return boardPostService.updateSupportPost(id, request, admin);
    }

    @PatchMapping("/{id}/ouid-dispute")
    public BoardPostResponseDto resolveOuidDispute(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id,
            @RequestBody OuidDisputeResolutionRequest request) {
        AuthUserResponse admin = authService.requireAdmin(bearerToken(authorization));
        return boardPostService.resolveOuidDispute(id, request, admin);
    }

    @PatchMapping("/{id}/claimed-owner-status")
    public BoardPostResponseDto updateClaimedOwnerStatus(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id,
            @RequestBody AccountStatusUpdateRequest request) {
        AuthUserResponse admin = authService.requireAdmin(bearerToken(authorization));
        return boardPostService.updateClaimedOwnerStatus(id, request, admin);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("id") Long id) {
        authService.requireAdmin(bearerToken(authorization));
        boardImageStorageService.delete(boardPostService.deletePost(id));
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
