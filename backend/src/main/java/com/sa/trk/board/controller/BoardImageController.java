package com.sa.trk.board.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.board.service.BoardImageStorageService;
import com.sa.trk.board.service.BoardImageStorageService.StoredImage;
import com.sa.trk.board.service.BoardPostService;
import com.sa.trk.board.service.BoardPostService.BoardImageAccess;
import com.sa.trk.auth.dto.AuthUserResponse;
import com.sa.trk.auth.service.AuthService;

@RestController
@RequestMapping("/api/board/images")
public class BoardImageController {

    private final BoardImageStorageService boardImageStorageService;
    private final BoardPostService boardPostService;
    private final AuthService authService;

    public BoardImageController(
            BoardImageStorageService boardImageStorageService,
            BoardPostService boardPostService,
            AuthService authService) {
        this.boardImageStorageService = boardImageStorageService;
        this.boardPostService = boardPostService;
        this.authService = authService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getImage(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable("fileName") String fileName) {
        String imageUrl = "/api/board/images/" + fileName;
        BoardImageAccess access = boardPostService.getImageAccess(imageUrl);
        AuthUserResponse viewer = access.privatePost()
                ? authService.currentUser(bearerToken(authorization))
                : null;
        boardPostService.requireImageAccess(access, viewer);
        StoredImage storedImage = boardImageStorageService.load(fileName);
        return ResponseEntity.ok()
                .cacheControl(access.privatePost() ? CacheControl.noStore() : CacheControl.noCache())
                .contentType(storedImage.mediaType())
                .body(storedImage.resource());
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        return authorization.substring(7).trim();
    }
}
