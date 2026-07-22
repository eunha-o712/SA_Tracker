package com.sa.trk.board.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class BoardImageStorageService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final long MAX_IMAGE_SIZE = 8L * 1024L * 1024L;
    private static final String PUBLIC_PATH = "/api/board/images/";

    private final Path storageDirectory;

    public BoardImageStorageService() {
        this.storageDirectory = Path.of(System.getProperty("user.dir"), "uploads", "board")
                .toAbsolutePath()
                .normalize();
    }

    public List<String> store(List<MultipartFile> images) {
        List<MultipartFile> validImages = images == null
                ? List.of()
                : images.stream().filter(image -> image != null && !image.isEmpty()).toList();

        if (validImages.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("이미지는 최대 5장까지 첨부할 수 있습니다.");
        }

        List<String> storedUrls = new ArrayList<>();
        try {
            Files.createDirectories(storageDirectory);
            for (MultipartFile image : validImages) {
                storedUrls.add(storeOne(image));
            }
            return List.copyOf(storedUrls);
        } catch (RuntimeException | IOException exception) {
            delete(storedUrls);
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("이미지를 저장하지 못했습니다.", exception);
        }
    }

    public StoredImage load(String fileName) {
        String normalizedName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (!normalizedName.matches("[0-9a-f-]{36}\\.(jpg|png|gif|webp)")) {
            throw new IllegalArgumentException("올바르지 않은 이미지 경로입니다.");
        }

        try {
            Path imagePath = storageDirectory.resolve(normalizedName).normalize();
            if (!imagePath.startsWith(storageDirectory) || !Files.isRegularFile(imagePath)) {
                throw new IllegalArgumentException("이미지를 찾을 수 없습니다.");
            }
            Resource resource = new UrlResource(imagePath.toUri());
            return new StoredImage(resource, mediaTypeFor(normalizedName));
        } catch (IOException exception) {
            throw new IllegalStateException("이미지를 불러오지 못했습니다.", exception);
        }
    }

    public void delete(List<String> imageUrls) {
        if (imageUrls == null) return;
        for (String imageUrl : imageUrls) {
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                Path imagePath = storageDirectory.resolve(fileName).normalize();
                if (imagePath.startsWith(storageDirectory)) {
                    Files.deleteIfExists(imagePath);
                }
            } catch (RuntimeException | IOException ignored) {
                // Best-effort cleanup for an incomplete post submission.
            }
        }
    }

    private String storeOne(MultipartFile image) throws IOException {
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("이미지 한 장의 크기는 8MB 이하여야 합니다.");
        }

        byte[] bytes = image.getBytes();
        ImageFormat format = detectFormat(bytes);
        if (format == null) {
            throw new IllegalArgumentException("JPG, PNG, GIF, WEBP 이미지만 첨부할 수 있습니다.");
        }

        String fileName = UUID.randomUUID() + format.extension();
        Path target = storageDirectory.resolve(fileName).normalize();
        if (!target.startsWith(storageDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 이미지 경로입니다.");
        }
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        return PUBLIC_PATH + fileName;
    }

    private ImageFormat detectFormat(byte[] bytes) {
        if (bytes.length >= 8
                && unsigned(bytes[0]) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && unsigned(bytes[4]) == 0x0D && unsigned(bytes[5]) == 0x0A
                && unsigned(bytes[6]) == 0x1A && unsigned(bytes[7]) == 0x0A) {
            return ImageFormat.PNG;
        }
        if (bytes.length >= 3 && unsigned(bytes[0]) == 0xFF && unsigned(bytes[1]) == 0xD8 && unsigned(bytes[2]) == 0xFF) {
            return ImageFormat.JPG;
        }
        if (bytes.length >= 6) {
            String header = new String(bytes, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
            if ("GIF87a".equals(header) || "GIF89a".equals(header)) return ImageFormat.GIF;
        }
        if (bytes.length >= 12) {
            String riff = new String(bytes, 0, 4, java.nio.charset.StandardCharsets.US_ASCII);
            String webp = new String(bytes, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
            if ("RIFF".equals(riff) && "WEBP".equals(webp)) return ImageFormat.WEBP;
        }
        return null;
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private MediaType mediaTypeFor(String fileName) {
        if (fileName.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (fileName.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (fileName.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    private enum ImageFormat {
        JPG(".jpg"), PNG(".png"), GIF(".gif"), WEBP(".webp");

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
