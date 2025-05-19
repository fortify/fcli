package com.fortify.cli.aviator._common.util;

import java.util.AbstractMap;
import java.util.Map;
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
            String jsonString = JsonFormat.printer()
                    .includingDefaultValueFields()
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
        message.getAllFields().entrySet().forEach(e->
            LogMaskHelper.INSTANCE.registerValue(maskedFields.get(e.getKey().getJsonName()), LogMaskSource.GRPC_RESPONSE, e.getValue()));
    }

    public static JsonNode emptyJsonNode() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }
}