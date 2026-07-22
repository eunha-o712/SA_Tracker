package com.sa.trk.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LocalEnvLoader {

    private LocalEnvLoader() {
    }

    public static Map<String, Object> load() {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (Path candidate : candidates()) {
            if (Files.isRegularFile(candidate)) {
                read(candidate, properties);
                break;
            }
        }
        return properties;
    }

    private static List<Path> candidates() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        Path parent = cwd.getParent();

        return parent == null
                ? List.of(cwd.resolve(".env.local"), cwd.resolve("backend").resolve(".env.local"))
                : List.of(
                        cwd.resolve(".env.local"),
                        cwd.resolve("backend").resolve(".env.local"),
                        parent.resolve("backend").resolve(".env.local")
                );
    }

    private static void read(Path path, Map<String, Object> properties) {
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                parseLine(line, properties);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("로컬 환경변수 파일을 읽을 수 없습니다.", exception);
        }
    }

    private static void parseLine(String line, Map<String, Object> properties) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int delimiter = trimmed.indexOf('=');
        if (delimiter <= 0) {
            return;
        }

        String name = trimmed.substring(0, delimiter).trim();
        String value = unquote(trimmed.substring(delimiter + 1).trim());
        if (name.isEmpty() || value.isEmpty()) {
            return;
        }

        properties.putIfAbsent(name, value);
        String propertyName = toSpringPropertyName(name);
        if (propertyName != null) {
            properties.putIfAbsent(propertyName, value);
        }
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static String toSpringPropertyName(String name) {
        return switch (name) {
            case "OPENAI_API_KEY" -> "openai.api.key";
            case "OPENAI_MODEL" -> "openai.api.model";
            case "OPENAI_BASE_URL" -> "openai.api.base-url";
            default -> null;
        };
    }
}
