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
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class NativeReflectConfigTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String SPEL_REFLECT_CONFIG = "META-INF/native-image/fcli/fcli-app/spel/reflect-config.json";
    private static final String YAML_REFLECT_CONFIG = "META-INF/native-image/fcli/fcli-app/yaml/reflect-config.json";
    private static final String GRPC_REFLECT_CONFIG = "META-INF/native-image/fcli/fcli-app/grpc/reflect-config.json";
    private static final String OFFSET_DATE_TIME_CLASS = "java.time.OffsetDateTime";
    private static final String TAG_MAPPING_CONFIG_CLASS = "com.fortify.cli.aviator.config.TagMappingConfig";
    private static final List<String> TAG_MAPPING_NESTED_CLASSES = List.of(
            "com.fortify.cli.aviator.config.TagMappingConfig$SuppressionExclusion",
            "com.fortify.cli.aviator.config.TagMappingConfig$Mapping",
            "com.fortify.cli.aviator.config.TagMappingConfig$ProductMapping",
            "com.fortify.cli.aviator.config.TagMappingConfig$Tier",
            "com.fortify.cli.aviator.config.TagMappingConfig$Result");

    @ParameterizedTest
    @MethodSource("getReflectConfigContracts")
    void testNativeReflectConfigContracts(String resourcePath, String className,
            boolean expectAllDeclaredFields, boolean expectAllPublicMethods,
            List<String> expectedMethods, List<String> expectedEntries) throws Exception {
        JsonNode reflectConfig = loadReflectConfig(resourcePath);
        JsonNode reflectConfigEntry = getReflectConfigEntry(reflectConfig, className);

        if ( expectAllDeclaredFields ) {
            assertTrue(reflectConfigEntry.path("allDeclaredFields").asBoolean(),
                    () -> "Expected allDeclaredFields for " + className + " in " + resourcePath);
        }
        if ( expectAllPublicMethods ) {
            assertTrue(reflectConfigEntry.path("allPublicMethods").asBoolean(),
                    () -> "Expected allPublicMethods for " + className + " in " + resourcePath);
        }
        expectedMethods.forEach(methodName -> assertTrue(hasMethod(reflectConfigEntry, methodName),
                () -> "Expected " + methodName + " metadata for " + className + " in " + resourcePath));
        expectedEntries.forEach(expectedEntry -> assertTrue(hasReflectConfigEntry(reflectConfig, expectedEntry),
                () -> "Expected reflect-config entry for " + expectedEntry + " in " + resourcePath));
    }

    private static Stream<Arguments> getReflectConfigContracts() {
        return Stream.of(
                Arguments.of(SPEL_REFLECT_CONFIG, OFFSET_DATE_TIME_CLASS, false, true, List.of(), List.of()),
                Arguments.of(YAML_REFLECT_CONFIG, TAG_MAPPING_CONFIG_CLASS, true, false,
                        List.of("setSuppression_exclusions"), TAG_MAPPING_NESTED_CLASSES),
                Arguments.of(GRPC_REFLECT_CONFIG, TAG_MAPPING_CONFIG_CLASS, true, false,
                        List.of("setSuppression_exclusions"), TAG_MAPPING_NESTED_CLASSES));
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