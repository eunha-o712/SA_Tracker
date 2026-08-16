package com.sa.trk.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Map;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.sa.trk.config.CloudinaryProperties;

class ProfileImageStorageServiceTests {

    private static final String PUBLIC_ID = "satrk/profile/6f906724-5a9e-4ec3-b2d9-dc9dc7bd67a1";
    private static final String SECURE_URL =
            "https://res.cloudinary.com/demo/image/upload/v1776031200/" + PUBLIC_ID + ".png";

    @TempDir
    Path temporaryDirectory;

    private Uploader uploader;
    private ProfileImageStorageService storageService;

    @BeforeEach
    void setUp() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("demo");
        properties.setApiKey("api-key");
        properties.setApiSecret("api-secret");

        Cloudinary cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        storageService = new ProfileImageStorageService(properties, cloudinary, temporaryDirectory);
    }

    @Test
    void storesProfileImageInManagedCloudinaryFolder() throws Exception {
        byte[] png = new byte[] {
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
        };
        MockMultipartFile image = new MockMultipartFile("image", "profile.png", "image/png", png);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of("secure_url", SECURE_URL));

        String storedUrl = storageService.store(image);

        assertThat(storedUrl).isEqualTo(SECURE_URL);
        verify(uploader).upload(any(byte[].class), anyMap());
    }

    @Test
    void deletesOnlyManagedCloudinaryProfileImage() throws Exception {
        storageService.delete(SECURE_URL);

        verify(uploader).destroy(eq(PUBLIC_ID), anyMap());
    }
}
