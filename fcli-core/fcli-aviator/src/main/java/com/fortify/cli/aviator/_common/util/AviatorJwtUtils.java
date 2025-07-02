package com.fortify.cli.aviator._common.util;

import java.util.Base64;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.util.StringUtils; // For StringUtils.isBlank

public final class AviatorJwtUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorJwtUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AviatorJwtUtils() {}

    private static JsonNode getJwtPayload(String token) {
        if (StringUtils.isBlank(token)) {
            LOG.warn("WARN: Provided token is null or blank, cannot extract payload.");
            return null;
        }
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length < 2) {
                LOG.warn("WARN: Invalid JWT token structure ({} parts), cannot extract payload.", chunks.length);
                return null;
            }
            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payloadJson = new String(decoder.decode(chunks[1]));
            return objectMapper.readTree(payloadJson);
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to decode Base64URL from JWT payload: {}. Token part: {}", e.getMessage(), (token.split("\\.").length > 1 ? token.split("\\.")[1] : "N/A"));
            LOG.debug("Base64 Decode Exception details: ", e);
        } catch (Exception e) {
            LOG.warn("Failed to parse JWT token payload: {}.", e.getMessage());
            LOG.debug("JWT Parse Exception details: ", e);
        }
        return null;
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
                    LOG.warn("Token 'exp' field is present but not a positive value: {}", expSeconds);
                }
            } else {
                LOG.warn("Token 'exp' field is present but not a numeric value: {}", expNode.asText());
            }
        } else {
            LOG.warn("Token payload does not contain 'exp' field.");
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
                LOG.warn("Token 'sub' field is present but not a non-blank text value: {}", subNode.asText(""));
            }
        }

        if (StringUtils.isBlank(email)) {
            LOG.warn("WARN: Token payload does not contain a usable 'email' or 'sub' field.");
        }
        return email;
    }
}