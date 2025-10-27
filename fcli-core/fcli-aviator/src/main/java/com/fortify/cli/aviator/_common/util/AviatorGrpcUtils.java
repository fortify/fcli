/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator._common.util;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValueDescriptor;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class AviatorGrpcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Map<String, MaskValueDescriptor> maskedFields = Stream.of(
            new AbstractMap.SimpleImmutableEntry<>("token", new MaskValueDescriptor(LogSensitivityLevel.high, "AVIATOR TOKEN"))
            ).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

    public static JsonNode grpcToJsonNode(Message message) {
        registerLogMaskFields(message);
        try {
            Set<Descriptors.FieldDescriptor> allFields =
                    new HashSet<>(message.getDescriptorForType().getFields());

            String jsonString = JsonFormat.printer()
                    .includingDefaultValueFields(allFields)
                    .preservingProtoFieldNames()
                    .print(message);
            LOG.debug("Converted gRPC message to JSON: {}", jsonString);
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            LOG.error("Error converting gRPC message to JSON: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to convert gRPC message to JSON", e);
        }
    }

    private static void registerLogMaskFields(Message message) {
        message.getAllFields().forEach((key, value) -> LogMaskHelper.INSTANCE.registerValue(maskedFields.get(key.getJsonName()), LogMaskSource.GRPC_RESPONSE, value));
    }

    public static JsonNode emptyJsonNode() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }
}