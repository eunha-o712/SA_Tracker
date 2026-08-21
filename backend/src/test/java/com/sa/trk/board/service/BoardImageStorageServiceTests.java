package com.sa.trk.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.cloudinary.utils.ObjectUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.sa.trk.config.CloudinaryProperties;

class BoardImageStorageServiceTests {

    private static final String FILE_NAME = "6f906724-5a9e-4ec3-b2d9-dc9dc7bd67a1.png";
    private static final String PUBLIC_ID = "satrk/board/6f906724-5a9e-4ec3-b2d9-dc9dc7bd67a1";
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A
    };

    @TempDir
    Path temporaryDirectory;

    private Cloudinary cloudinary;
    private Uploader uploader;
    private BoardImageStorageService storageService;

    @BeforeEach
    void setUp() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("demo");
        properties.setApiKey("api-key");
        properties.setApiSecret("api-secret");

        cloudinary = mock(Cloudinary.class);
        uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        storageService = new BoardImageStorageService(
                properties,
                cloudinary,
                temporaryDirectory,
                uri -> PNG
        );
    }

    @Test
    void storesNewBoardImageAsAuthenticatedCloudinaryAsset() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "evidence.png", "image/png", PNG);
        when(uploader.upload(any(byte[].class), anyMap())).thenAnswer(invocation -> {
            Map<?, ?> options = invocation.getArgument(1);
            return Map.of("public_id", options.get("public_id"));
        });

        List<String> storedUrls = storageService.store(List.of(image));

        assertThat(storedUrls).singleElement().asString()
                .matches("/api/board/images/[0-9a-f-]{36}\\.png");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue())
                .containsEntry("resource_type", "image")
                .containsEntry("type", "authenticated")
                .containsEntry("format", "png")
                .containsEntry("overwrite", false);
        assertThat(String.valueOf(optionsCaptor.getValue().get("public_id")))
                .startsWith("satrk/board/");
    }

    @Test
    void loadsCloudinaryImageThroughTemporaryAuthenticatedDownloadUrl() throws Exception {
        String downloadUrl = "https://api.cloudinary.com/private-download/example";
        AtomicReference<URI> fetchedUri = new AtomicReference<>();
        storageService = storageServiceWithFetcher(uri -> {
            fetchedUri.set(uri);
            return PNG;
        });
        when(cloudinary.privateDownload(eq(PUBLIC_ID), eq("png"), anyMap())).thenReturn(downloadUrl);

        BoardImageStorageService.StoredImage storedImage = storageService.load(FILE_NAME);

        assertThat(storedImage.mediaType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(storedImage.resource().getContentAsByteArray()).isEqualTo(PNG);
        assertThat(fetchedUri.get()).isEqualTo(URI.create(downloadUrl));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(cloudinary).privateDownload(eq(PUBLIC_ID), eq("png"), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue())
                .containsEntry("resource_type", "image")
                .containsEntry("type", "authenticated")
                .containsKey("expires_at");
    }

    @Test
    void generatesValidPrivateDownloadUrlWithCloudinaryJavaSdk() {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("demo");
        properties.setApiKey("api-key");
        properties.setApiSecret("api-secret");
        Cloudinary realCloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.getCloudName(),
                "api_key", properties.getApiKey(),
                "api_secret", properties.getApiSecret(),
                "secure", true
        ));
        AtomicReference<URI> fetchedUri = new AtomicReference<>();
        BoardImageStorageService serviceWithRealSdk = new BoardImageStorageService(
                properties,
                realCloudinary,
                temporaryDirectory,
                uri -> {
                    fetchedUri.set(uri);
                    return PNG;
                }
        );

        serviceWithRealSdk.load(FILE_NAME);

        assertThat(fetchedUri.get())
                .hasScheme("https")
                .hasHost("api.cloudinary.com")
                .hasPath("/v1_1/demo/image/download");
        assertThat(fetchedUri.get().getQuery())
                .contains("type=authenticated")
                .contains("public_id=satrk/board/6f906724-5a9e-4ec3-b2d9-dc9dc7bd67a1")
                .contains("signature=");
    }

    @Test
    void keepsServingLegacyLocalBoardImages() throws Exception {
        Files.write(temporaryDirectory.resolve(FILE_NAME), PNG);

        BoardImageStorageService.StoredImage storedImage = storageService.load(FILE_NAME);

        assertThat(storedImage.resource().getContentAsByteArray()).isEqualTo(PNG);
        verify(cloudinary, never()).privateDownload(any(), any(), anyMap());
    }

    @Test
    void deletesAuthenticatedCloudinaryAssetByInternalBoardUrl() throws Exception {
        storageService.delete(List.of("/api/board/images/" + FILE_NAME));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> optionsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(uploader).destroy(eq(PUBLIC_ID), optionsCaptor.capture());
        assertThat(optionsCaptor.getValue())
                .containsEntry("resource_type", "image")
                .containsEntry("type", "authenticated")
                .containsEntry("invalidate", true);
    }

    private BoardImageStorageService storageServiceWithFetcher(
            BoardImageStorageService.RemoteImageFetcher fetcher) {
        CloudinaryProperties properties = new CloudinaryProperties();
        properties.setCloudName("demo");
        properties.setApiKey("api-key");
        properties.setApiSecret("api-secret");
        return new BoardImageStorageService(properties, cloudinary, temporaryDirectory, fetcher);
    }
}
