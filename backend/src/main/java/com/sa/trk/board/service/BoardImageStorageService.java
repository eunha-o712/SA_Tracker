package com.sa.trk.board.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sa.trk.config.CloudinaryProperties;

@Service
public class BoardImageStorageService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final long MAX_IMAGE_SIZE = 8L * 1024L * 1024L;
    private static final String PUBLIC_PATH = "/api/board/images/";
    private static final String CLOUDINARY_FOLDER = "satrk/board";
    private static final String AUTHENTICATED_DELIVERY_TYPE = "authenticated";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(15);
    private static final long DOWNLOAD_URL_LIFETIME_SECONDS = 60L;

    private final CloudinaryProperties properties;
    private final Cloudinary cloudinary;
    private final Path legacyStorageDirectory;
    private final RemoteImageFetcher remoteImageFetcher;

    @Autowired
    public BoardImageStorageService(CloudinaryProperties properties) {
        this(
                properties,
                createClient(properties),
                Path.of(System.getProperty("user.dir"), "uploads", "board"),
                createRemoteImageFetcher()
        );
    }

    BoardImageStorageService(
            CloudinaryProperties properties,
            Cloudinary cloudinary,
            Path legacyStorageDirectory,
            RemoteImageFetcher remoteImageFetcher) {
        this.properties = properties;
        this.cloudinary = cloudinary;
        this.legacyStorageDirectory = legacyStorageDirectory.toAbsolutePath().normalize();
        this.remoteImageFetcher = remoteImageFetcher;
    }

    public List<String> store(List<MultipartFile> images) {
        List<MultipartFile> validImages = images == null
                ? List.of()
                : images.stream().filter(image -> image != null && !image.isEmpty()).toList();

        if (validImages.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("이미지는 최대 5장까지 첨부할 수 있습니다.");
        }
        if (validImages.isEmpty()) {
            return List.of();
        }
        requireCloudinary();

        List<String> storedUrls = new ArrayList<>();
        try {
            for (MultipartFile image : validImages) {
                storedUrls.add(storeOne(image));
            }
            return List.copyOf(storedUrls);
        } catch (RuntimeException | IOException exception) {
            delete(storedUrls);
            if (exception instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }
            throw new IllegalStateException("이미지를 영구 저장소에 저장하지 못했습니다.", exception);
        }
    }

    public StoredImage load(String fileName) {
        String normalizedName = normalizeFileName(fileName);
        StoredImage legacyImage = loadLegacyImage(normalizedName);
        if (legacyImage != null) {
            return legacyImage;
        }

        requireCloudinary();
        try {
            ImageIdentifier identifier = imageIdentifier(normalizedName);
            Map<String, Object> downloadOptions = Map.of(
                    "resource_type", "image",
                    "type", AUTHENTICATED_DELIVERY_TYPE,
                    "expires_at", Instant.now().getEpochSecond() + DOWNLOAD_URL_LIFETIME_SECONDS
            );
            String downloadUrl = cloudinary.privateDownload(
                    identifier.publicId(),
                    identifier.format(),
                    downloadOptions
            );
            URI downloadUri = requireCloudinaryDownloadUri(downloadUrl);
            byte[] bytes = remoteImageFetcher.fetch(downloadUri);
            if (bytes.length == 0 || bytes.length > MAX_IMAGE_SIZE) {
                throw new IllegalStateException("저장된 이미지의 크기가 올바르지 않습니다.");
            }
            ImageFormat actualFormat = detectFormat(bytes);
            if (actualFormat == null || !actualFormat.extension().equals(identifier.extension())) {
                throw new IllegalStateException("저장된 이미지 형식을 확인할 수 없습니다.");
            }
            return new StoredImage(new ByteArrayResource(bytes), mediaTypeFor(normalizedName));
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("이미지를 불러오지 못했습니다.", exception);
        }
    }

    public void delete(List<String> imageUrls) {
        if (imageUrls == null) return;
        for (String imageUrl : imageUrls) {
            deleteOne(imageUrl);
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

        String assetId = UUID.randomUUID().toString();
        String publicId = CLOUDINARY_FOLDER + "/" + assetId;
        Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                "public_id", publicId,
                "resource_type", "image",
                "type", AUTHENTICATED_DELIVERY_TYPE,
                "format", format.format(),
                "overwrite", false,
                "discard_original_filename", true
        ));
        String storedPublicId = Objects.toString(result.get("public_id"), "").trim();
        if (!publicId.equals(storedPublicId)) {
            throw new IllegalStateException("Cloudinary 이미지 식별자를 확인하지 못했습니다.");
        }
        return PUBLIC_PATH + assetId + format.extension();
    }

    private void deleteOne(String imageUrl) {
        try {
            String fileName = extractFileName(imageUrl);
            if (fileName == null) return;

            Path imagePath = legacyStorageDirectory.resolve(fileName).normalize();
            if (imagePath.startsWith(legacyStorageDirectory)) {
                Files.deleteIfExists(imagePath);
            }

            if (cloudinary != null && properties.isConfigured()) {
                cloudinary.uploader().destroy(imageIdentifier(fileName).publicId(), ObjectUtils.asMap(
                        "resource_type", "image",
                        "type", AUTHENTICATED_DELIVERY_TYPE,
                        "invalidate", true
                ));
            }
        } catch (RuntimeException | IOException ignored) {
            // 게시글 저장 실패나 삭제 후의 정리는 가능한 범위에서 수행합니다.
        }
    }

    private StoredImage loadLegacyImage(String normalizedName) {
        try {
            Path imagePath = legacyStorageDirectory.resolve(normalizedName).normalize();
            if (!imagePath.startsWith(legacyStorageDirectory) || !Files.isRegularFile(imagePath)) {
                return null;
            }
            return new StoredImage(new UrlResource(imagePath.toUri()), mediaTypeFor(normalizedName));
        } catch (IOException exception) {
            throw new IllegalStateException("기존 이미지를 불러오지 못했습니다.", exception);
        }
    }

    private String extractFileName(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(PUBLIC_PATH)) return null;
        String fileName = imageUrl.substring(PUBLIC_PATH.length());
        try {
            return normalizeFileName(fileName);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private ImageIdentifier imageIdentifier(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        String assetId = fileName.substring(0, extensionIndex);
        String extension = fileName.substring(extensionIndex);
        return new ImageIdentifier(CLOUDINARY_FOLDER + "/" + assetId, extension.substring(1), extension);
    }

    private String normalizeFileName(String fileName) {
        String normalizedName = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        if (!normalizedName.matches("[0-9a-f-]{36}\\.(jpg|png|gif|webp)")) {
            throw new IllegalArgumentException("올바르지 않은 이미지 경로입니다.");
        }
        return normalizedName;
    }

    private void requireCloudinary() {
        if (cloudinary == null || properties == null || !properties.isConfigured()) {
            throw new IllegalStateException("이미지 영구 저장소가 설정되지 않았습니다.");
        }
    }

    private URI requireCloudinaryDownloadUri(String downloadUrl) {
        URI uri = URI.create(downloadUrl == null ? "" : downloadUrl.trim());
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || host == null
                || !(host.equalsIgnoreCase("api.cloudinary.com")
                || host.toLowerCase(Locale.ROOT).endsWith(".cloudinary.com"))) {
            throw new IllegalStateException("Cloudinary 다운로드 주소를 확인하지 못했습니다.");
        }
        return uri;
    }

    private static Cloudinary createClient(CloudinaryProperties properties) {
        if (properties == null || !properties.isConfigured()) return null;
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
    }

    private static RemoteImageFetcher createRemoteImageFetcher() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(DOWNLOAD_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        return uri -> {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<byte[]> response;
            try {
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("Cloudinary 이미지 다운로드가 중단되었습니다.", exception);
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Cloudinary 이미지 다운로드에 실패했습니다: HTTP " + response.statusCode());
            }
            return response.body();
        };
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
        if (bytes.length >= 6) {
            String header = new String(bytes, 0, 6, StandardCharsets.US_ASCII);
            if ("GIF87a".equals(header) || "GIF89a".equals(header)) return ImageFormat.GIF;
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
        if (fileName.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (fileName.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        return MediaType.IMAGE_JPEG;
    }

    private enum ImageFormat {
        JPG("jpg", ".jpg"), PNG("png", ".png"), GIF("gif", ".gif"), WEBP("webp", ".webp");

        private final String format;
        private final String extension;

        ImageFormat(String format, String extension) {
            this.format = format;
            this.extension = extension;
        }

        String format() {
            return format;
        }

        String extension() {
            return extension;
        }
    }

    @FunctionalInterface
    interface RemoteImageFetcher {
        byte[] fetch(URI uri) throws IOException;
    }

    private record ImageIdentifier(String publicId, String format, String extension) {
    }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }
}
