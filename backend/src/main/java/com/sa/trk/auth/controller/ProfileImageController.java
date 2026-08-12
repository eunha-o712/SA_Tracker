package com.sa.trk.auth.controller;

import java.time.Duration;

import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sa.trk.auth.service.ProfileImageStorageService;
import com.sa.trk.auth.service.ProfileImageStorageService.StoredImage;

@RestController
@RequestMapping("/api/profile-images")
public class ProfileImageController {

    private final ProfileImageStorageService profileImageStorageService;

    public ProfileImageController(ProfileImageStorageService profileImageStorageService) {
        this.profileImageStorageService = profileImageStorageService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable("fileName") String fileName) {
        StoredImage storedImage = profileImageStorageService.load(fileName);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .contentType(storedImage.mediaType())
                .body(storedImage.resource());
    }
}
