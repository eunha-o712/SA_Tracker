package com.sa.trk.auth.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProfileImageStorageService {

    private static final long MAX_IMAGE_SIZE = 3L * 1024L * 1024L;
    private static final String PUBLIC_PATH = "/api/profile-images/";

    private final Path storageDirectory;

    public ProfileImageStorageService() {
        this.storageDirectory = Path.of(System.getProperty("user.dir"), "uploads", "profile")
                .toAbsolutePath()
                .normalize();
    }

    public String store(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("프로필 이미지를 선택해 주세요.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("프로필 이미지는 3MB 이하여야 합니다.");
        }

        try {
            byte[] bytes = image.getBytes();
            ImageFormat format = detectFormat(bytes);
            if (format == null) {
                throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 사용할 수 있습니다.");
            }

            Files.createDirectories(storageDirectory);
            String fileName = UUID.randomUUID() + format.extension();
            Path target = storageDirectory.resolve(fileName).normalize();
            if (!target.startsWith(storageDirectory)) {
                throw new IllegalArgumentException("올바르지 않은 프로필 이미지 경로입니다.");
            }
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
            return PUBLIC_PATH + fileName;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("프로필 이미지를 저장하지 못했습니다.", exception);
        }
    }

    public StoredImage load(String fileName) {
        String normalizedName = normalizeFileName(fileName);
        try {
            Path imagePath = storageDirectory.resolve(normalizedName).normalize();
            if (!imagePath.startsWith(storageDirectory) || !Files.isRegularFile(imagePath)) {
                throw new IllegalArgumentException("프로필 이미지를 찾을 수 없습니다.");
            }
            return new StoredImage(new UrlResource(imagePath.toUri()), mediaTypeFor(normalizedName));
        } catch (IOException exception) {
            throw new IllegalStateException("프로필 이미지를 불러오지 못했습니다.", exception);
        }
    }

    public void delete(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank() || !imageUrl.startsWith(PUBLIC_PATH)) return;
        try {
            String fileName = normalizeFileName(imageUrl.substring(PUBLIC_PATH.length()));
            Path imagePath = storageDirectory.resolve(fileName).normalize();
            if (imagePath.startsWith(storageDirectory)) {
                Files.deleteIfExists(imagePath);
            }
        } catch (RuntimeException | IOException ignored) {
            // Best-effort cleanup after replacing or clearing a profile image.
        }
    }

    private String normalizeFileName(String fileName) {
        String normalizedName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (!normalizedName.matches("[0-9a-f-]{36}\\.(jpg|png|webp)")) {
            throw new IllegalArgumentException("올바르지 않은 프로필 이미지 경로입니다.");
        }
        return normalizedName;
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && unsigned(bytes[0]) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && unsigned(bytes[4]) == 0x0D && unsigned(bytes[5]) == 0x0A
                && unsigned(bytes[6]) == 0x1A && unsigned(bytes[7]) == 0x0A) {
            return ImageFormat.PNG;
        }
        if (bytes.length >= 3
                && unsigned(bytes[0]) == 0xFF
                && unsigned(bytes[1]) == 0xD8
                && unsigned(bytes[2]) == 0xFF) {
            return ImageFormat.JPG;
        }
        if (bytes.length >= 12) {
            String riff = new String(bytes, 0, 4, StandardCharsets.US_ASCII);
            String webp = new String(bytes, 8, 4, StandardCharsets.US_ASCII);
            if ("RIFF".equals(riff) && "WEBP".equals(webp)) return ImageFormat.WEBP;
        }
        return null;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private MediaType mediaTypeFor(String fileName) {
        if (fileName.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (fileName.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    private enum ImageFormat {
        JPG(".jpg"), PNG(".png"), WEBP(".webp");

        private final String extension;

        ImageFormat(String extension) {
            this.extension = extension;
        }

        String extension() {
            return extension;
        }
    }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }
}
