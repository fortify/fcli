/*
 * Copyright 2021-2026 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.StreamSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class NativeYamlReflectConfigTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TAG_MAPPING_CONFIG_CLASS = "com.fortify.cli.aviator.config.TagMappingConfig";
    private static final List<String> TAG_MAPPING_NESTED_CLASSES = List.of(
            "com.fortify.cli.aviator.config.TagMappingConfig$SuppressionExclusion",
            "com.fortify.cli.aviator.config.TagMappingConfig$Mapping",
            "com.fortify.cli.aviator.config.TagMappingConfig$Tier",
            "com.fortify.cli.aviator.config.TagMappingConfig$Result");

    @ParameterizedTest
    @ValueSource(strings = {
            "META-INF/native-image/fcli/fcli-app/yaml/reflect-config.json",
            "META-INF/native-image/fcli/fcli-app/grpc/reflect-config.json"
    })
    void testTagMappingConfigNativeReflectConfigIncludesSuppressionExclusions(String resourcePath) throws Exception {
        JsonNode reflectConfig = loadReflectConfig(resourcePath);
        JsonNode tagMappingConfigEntry = getReflectConfigEntry(reflectConfig, TAG_MAPPING_CONFIG_CLASS);

        assertTrue(tagMappingConfigEntry.path("allDeclaredFields").asBoolean(),
                () -> "Expected allDeclaredFields for " + TAG_MAPPING_CONFIG_CLASS + " in " + resourcePath);
        assertTrue(hasMethod(tagMappingConfigEntry, "setSuppression_exclusions"),
                () -> "Expected setSuppression_exclusions metadata for " + TAG_MAPPING_CONFIG_CLASS + " in " + resourcePath);

        TAG_MAPPING_NESTED_CLASSES.forEach(className -> assertTrue(hasReflectConfigEntry(reflectConfig, className),
                () -> "Expected reflect-config entry for " + className + " in " + resourcePath));
    }

    private JsonNode loadReflectConfig(String resourcePath) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            assertNotNull(inputStream, () -> "Missing native reflect-config resource: " + resourcePath);
            return OBJECT_MAPPER.readTree(inputStream);
        }
    }

    private JsonNode getReflectConfigEntry(JsonNode reflectConfig, String className) {
        return StreamSupport.stream(reflectConfig.spliterator(), false)
                .filter(node -> className.equals(node.path("name").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing reflect-config entry for " + className));
    }

    private boolean hasReflectConfigEntry(JsonNode reflectConfig, String className) {
        return StreamSupport.stream(reflectConfig.spliterator(), false)
                .anyMatch(node -> className.equals(node.path("name").asText()));
    }

    private boolean hasMethod(JsonNode reflectConfigEntry, String methodName) {
        return StreamSupport.stream(reflectConfigEntry.path("methods").spliterator(), false)
                .anyMatch(node -> methodName.equals(node.path("name").asText()));
    }
}
