package com.sa.trk.ai.service;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.sa.trk.config.OpenAiProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class OpenAiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final OpenAiProperties openAiProperties;

    public OpenAiClient(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            OpenAiProperties openAiProperties) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.openAiProperties = openAiProperties;
    }

    public String createTextResponse(String prompt) {
        if (isBlank(openAiProperties.getKey())) {
            throw new OpenAiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "OPENAI_KEY_MISSING",
                    "OpenAI API 키가 설정되지 않았습니다."
            );
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", resolveModel());
        body.put("input", prompt);
        body.put("max_output_tokens", 900);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    responsesUri(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers()),
                    JsonNode.class
            );
            return extractText(response.getBody());
        } catch (HttpStatusCodeException exception) {
            log.warn("OpenAI API error: status={}", exception.getStatusCode().value());
            throw new OpenAiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_API_ERROR",
                    "AI 분석 응답을 받아오지 못했습니다."
            );
        } catch (ResourceAccessException exception) {
            log.warn("OpenAI API unavailable");
            throw new OpenAiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_API_UNAVAILABLE",
                    "OpenAI API에 연결할 수 없습니다."
            );
        }
    }

    public String resolveModel() {
        return isBlank(openAiProperties.getModel())
                ? "gpt-4.1-mini"
                : openAiProperties.getModel().trim();
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openAiProperties.getKey().trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private URI responsesUri() {
        String baseUrl = isBlank(openAiProperties.getBaseUrl())
                ? "https://api.openai.com/v1"
                : openAiProperties.getBaseUrl().trim();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return URI.create(baseUrl + "/responses");
    }

    private String extractText(JsonNode body) {
        if (body == null) {
            throw new OpenAiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_EMPTY_RESPONSE",
                    "AI 분석 응답이 비어 있습니다."
            );
        }

        String directText = body.path("output_text").asText("");
        if (!directText.isBlank()) {
            return directText.trim();
        }

        StringBuilder builder = new StringBuilder();
        JsonNode output = body.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                JsonNode content = item.path("content");
                if (!content.isArray()) {
                    continue;
                }
                for (JsonNode part : content) {
                    if ("output_text".equals(part.path("type").asText())) {
                        String text = part.path("text").asText("");
                        if (!text.isBlank()) {
                            builder.append(text).append('\n');
                        }
                    }
                }
            }
        }

        String text = builder.toString().trim();
        if (text.isBlank()) {
            throw new OpenAiException(
                    HttpStatus.BAD_GATEWAY,
                    "OPENAI_TEXT_MISSING",
                    "AI 분석 텍스트를 찾을 수 없습니다."
            );
        }
        return text;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
