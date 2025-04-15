package com.fortify.cli.aviator._common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

public class AviatorGrpcUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper(); // Singleton instance

    public static JsonNode grpcToJsonNode(Message message) {
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

    public static JsonNode emptyJsonNode() {
        return objectMapper.createObjectNode();
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }
}