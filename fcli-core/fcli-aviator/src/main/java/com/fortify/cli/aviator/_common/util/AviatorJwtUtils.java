package com.fortify.cli.aviator._common.util;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

public final class AviatorJwtUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorJwtUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AviatorJwtUtils() {}

    private static JsonNode getJwtPayload(String token) {
        if (StringUtils.isBlank(token)) {
            throw new AviatorSimpleException("Provided token is null or blank, cannot extract payload.");
        }

        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                throw new IllegalArgumentException(String.format("Invalid token structure: expected at least 2 parts, but found %d.", chunks.length));
            }

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payloadJson;
            try {
                payloadJson = new String(decoder.decode(chunks[1]));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("The token's payload is not valid Base64URL.", e);
            }

            try {
                return objectMapper.readTree(payloadJson);
            } catch (IOException e) {
                throw new IllegalArgumentException("The token's payload is not valid JSON.", e);
            }
        } catch (IllegalArgumentException e) {
            throw new AviatorSimpleException("Invalid JWT token: failed to parse payload JSON.", e);
        }
    }

    public static Date extractExpiryDateFromToken(String token) {
        JsonNode payloadNode = getJwtPayload(token);
        if (payloadNode == null) {
            return null;
        }

        if (payloadNode.has("exp")) {
            JsonNode expNode = payloadNode.get("exp");
            if (expNode.isNumber()) {
                long expSeconds = expNode.asLong();
                if (expSeconds > 0) {
                    return new Date(expSeconds * 1000L);
                } else {
                    LOG.warn("WARN: Token 'exp' field is present but not a positive value: {}", expSeconds);
                }
            } else {
                LOG.warn("WARN: Token 'exp' field is present but not a numeric value: {}", expNode.asText());
            }
        } else {
            LOG.warn("WARN: Token payload does not contain 'exp' field.");
        }
        return null;
    }

    public static String extractTenantNameFromToken(String token) {
        JsonNode payloadNode = getJwtPayload(token);
        if (payloadNode == null) {
            return null;
        }

        if (payloadNode.has("tenantName")) {
            JsonNode tenantNameNode = payloadNode.get("tenantName");
            if (tenantNameNode.isTextual() && !StringUtils.isBlank(tenantNameNode.asText())) {
                return tenantNameNode.asText();
            } else {
                LOG.warn("WARN: Token 'tenantName' field is present but not a non-blank text value: {}", tenantNameNode.asText(""));
            }
        } else {
            LOG.warn("WARN: Token payload does not contain 'tenantName' field.");
        }
        return null;
    }

    public static String extractEmailFromToken(String token) {
        JsonNode payloadNode = getJwtPayload(token);
        if (payloadNode == null) {
            return null;
        }
        String email = null;
        if (payloadNode.has("email")) {
            JsonNode emailNode = payloadNode.get("email");
            if (emailNode.isTextual() && !StringUtils.isBlank(emailNode.asText())) {
                email = emailNode.asText();
            } else {
                LOG.warn("WARN: Token 'email' field is present but not a non-blank text value: {}", emailNode.asText(""));
            }
        }

        if (StringUtils.isBlank(email) && payloadNode.has("sub")) {
            JsonNode subNode = payloadNode.get("sub");
            if (subNode.isTextual() && !StringUtils.isBlank(subNode.asText())) {
                LOG.debug("Using 'sub' field as email as 'email' field is missing or blank.");
                email = subNode.asText();
            } else {
                LOG.warn("WARN: Token 'sub' field is present but not a non-blank text value: {}", subNode.asText(""));
            }
        }

        if (StringUtils.isBlank(email)) {
            LOG.warn("WARN: Token payload does not contain a usable 'email' or 'sub' field.");
        }
        return email;
    }
}