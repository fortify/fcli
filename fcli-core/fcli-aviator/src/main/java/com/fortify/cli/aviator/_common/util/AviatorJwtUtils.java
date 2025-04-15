package com.fortify.cli.aviator._common.util;

import java.util.Base64;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class AviatorJwtUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorJwtUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AviatorJwtUtils() {} // Private constructor for utility class

    public static Date extractExpiryDateFromToken(String token) {
        if (token == null) {
            return null;
        }
        try {
            String[] chunks = token.split("\\.");
            if (chunks.length >= 2) {
                Base64.Decoder decoder = Base64.getUrlDecoder();
                String payloadJson = new String(decoder.decode(chunks[1]));
                JsonNode payloadNode = objectMapper.readTree(payloadJson);

                if (payloadNode != null && payloadNode.has("exp")) {
                    long expSeconds = payloadNode.get("exp").asLong();
                    if (expSeconds > 0) {
                        return new Date(expSeconds * 1000L);
                    } else {
                        LOG.warn("Token 'exp' field is not a positive value: {}", expSeconds);
                    }
                } else {
                    LOG.warn("Token payload does not contain 'exp' field.");
                }
            } else {
                LOG.warn("Invalid JWT token structure, cannot extract expiry date.");
            }
        } catch (Exception e) {
            LOG.warn("Failed to decode JWT token or extract expiry date: {}.", e.getMessage());
            LOG.debug("JWT Decode Exception details: ", e);
        }
        return null;
    }
}