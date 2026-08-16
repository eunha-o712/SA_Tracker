package com.sa.trk.auth.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.sa.trk.config.CloudinaryProperties;

@Service
public class ProfileImageStorageService {

    private static final long MAX_IMAGE_SIZE = 3L * 1024L * 1024L;
    private static final String LOCAL_PUBLIC_PATH = "/api/profile-images/";
    private static final String CLOUDINARY_FOLDER = "satrk/profile";

    private final CloudinaryProperties properties;
    private final Cloudinary cloudinary;
    private final Path storageDirectory;
    private final Pattern cloudinaryImagePattern;

    @Autowired
    public ProfileImageStorageService(CloudinaryProperties properties) {
        this(
                properties,
                createClient(properties),
                Path.of(System.getProperty("user.dir"), "uploads", "profile")
        );
    }

    ProfileImageStorageService(
            CloudinaryProperties properties,
            Cloudinary cloudinary,
            Path storageDirectory) {
        this.properties = properties;
        this.cloudinary = cloudinary;
        this.storageDirectory = storageDirectory.toAbsolutePath().normalize();
        this.cloudinaryImagePattern = createManagedUrlPattern(properties.getCloudName());
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
            if (detectFormat(bytes) == null) {
                throw new IllegalArgumentException("JPG, PNG, WEBP 이미지만 사용할 수 있습니다.");
            }

            if (cloudinary == null || !properties.isConfigured()) {
                throw new IllegalStateException("프로필 이미지 클라우드 저장소가 설정되지 않았습니다.");
            }
            Map<?, ?> result = cloudinary.uploader().upload(bytes, ObjectUtils.asMap(
                    "folder", CLOUDINARY_FOLDER,
                    "public_id", UUID.randomUUID().toString(),
                    "resource_type", "image",
                    "overwrite", false
            ));
            String secureUrl = Objects.toString(result.get("secure_url"), "").trim();
            if (!cloudinaryImagePattern.matcher(secureUrl).matches()) {
                throw new IllegalStateException("Cloudinary 이미지 주소를 확인하지 못했습니다.");
            }
            return secureUrl;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new IllegalStateException("Cloudinary에 프로필 이미지를 저장하지 못했습니다.", exception);
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
        if (imageUrl == null || imageUrl.isBlank()) return;
        if (imageUrl.startsWith(LOCAL_PUBLIC_PATH)) {
            deleteLocalImage(imageUrl);
            return;
        }

        String publicId = extractCloudinaryPublicId(imageUrl);
        if (publicId == null || cloudinary == null) return;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (RuntimeException | IOException ignored) {
            // Best-effort cleanup after replacing or clearing a profile image.
        }
    }

    private void deleteLocalImage(String imageUrl) {
        try {
            String fileName = normalizeFileName(imageUrl.substring(LOCAL_PUBLIC_PATH.length()));
            Path imagePath = storageDirectory.resolve(fileName).normalize();
            if (imagePath.startsWith(storageDirectory)) {
                Files.deleteIfExists(imagePath);
            }
        } catch (RuntimeException | IOException ignored) {
            // Legacy local images remain readable while existing records are migrated.
        }
    }

    private String extractCloudinaryPublicId(String imageUrl) {
        try {
            URI uri = URI.create(imageUrl.trim());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
                return null;
            }
            Matcher matcher = cloudinaryImagePattern.matcher(imageUrl.trim());
            return matcher.matches() ? matcher.group(1) : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
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

    private static Pattern createManagedUrlPattern(String cloudName) {
        String quotedCloudName = Pattern.quote(cloudName == null ? "" : cloudName.trim());
        return Pattern.compile(
                "^https://res\\.cloudinary\\.com/" + quotedCloudName
                        + "/image/upload/(?:v\\d+/)?(satrk/profile/[0-9a-f-]{36})\\.(?:jpe?g|png|webp)$",
                Pattern.CASE_INSENSITIVE
        );
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

    private enum ImageFormat { JPG, PNG, WEBP }

    public record StoredImage(Resource resource, MediaType mediaType) {
    }
}
