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
package com.fortify.cli.ai_assist.mcp.helper.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

import io.modelcontextprotocol.common.McpTransportContext;

class MCPServerHttpSessionDescriptorResolverTest {
    @Test
    void createAuthCacheKeyHashesSscCredentials() {
        var config = sscConfig("https://ssc.example.com");
        var parser = new MCPServerHttpAuthHeaderParser(config);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var cacheKey = resolver.createAuthCacheKey(parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC,
            List.of("token=ssc-token;sc-sast-token=sast-token")
        ))));

        assertTrue(cacheKey.startsWith("ssc|"));
        assertFalse(cacheKey.contains("ssc-token"));
        assertFalse(cacheKey.contains("sast-token"));
    }

    @Test
    void createAuthCacheKeyHashesFoDClientCredentials() {
        var config = fodConfig("https://api.ams.fortify.com");
        var parser = new MCPServerHttpAuthHeaderParser(config);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var cacheKey = resolver.createAuthCacheKey(parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_FOD,
            List.of("client-id=client-id;client-secret=client-secret")
        ))));

        assertTrue(cacheKey.startsWith("fod-client|"));
        assertFalse(cacheKey.contains("client-id"));
        assertFalse(cacheKey.contains("client-secret"));
    }

    @Test
    void createAuthCacheKeyRejectsMixedFoDAuthModes() {
        var config = fodConfig("https://api.ams.fortify.com");
        var parser = new MCPServerHttpAuthHeaderParser(config);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var exception = assertThrows(FcliSimpleException.class, () -> resolver.createAuthCacheKey(parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_FOD,
            List.of("client-id=client-id;client-secret=client-secret;tenant=tenant;user=user;pat=pat")
        )))));

        assertTrue(exception.getMessage().contains("Specify either FoD client keys"));
    }

    @Test
    void createAuthCacheKeySupportsEscapedSemicolonBackslashAndEquals() {
        var config = sscConfig("https://ssc.example.com");
        var parser = new MCPServerHttpAuthHeaderParser(config);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var cacheKeyA = resolver.createAuthCacheKey(parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC,
            List.of("token=abc\\;def\\=ghi\\\\jkl;sc-sast-token=secondary")
        ))));
        var cacheKeyB = resolver.createAuthCacheKey(parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC,
            List.of("token=abc;sc-sast-token=secondary")
        ))));

        assertTrue(cacheKeyA.startsWith("ssc|"));
        assertFalse(cacheKeyA.contains("abc;def=ghi\\jkl"));
        assertFalse(cacheKeyA.equals(cacheKeyB));
    }

    @Test
    void createAuthCacheKeyRejectsInvalidEscapeSequence() {
        var config = sscConfig("https://ssc.example.com");
        var parser = new MCPServerHttpAuthHeaderParser(config);

        var exception = assertThrows(FcliSimpleException.class, () -> parser.parse(transportContext(Map.of(
            MCPServerHttpSessionDescriptorResolver.HEADER_AUTH_SSC,
            List.of("token=abc\\n")
        ))));

        assertEquals("Invalid X-AUTH-SSC header escape sequence '\\n'; supported escapes are \\\\, \\; and \\=", exception.getMessage());
    }

    private McpTransportContext transportContext(Map<String, List<String>> headers) {
        return McpTransportContext.create(Map.of("headers", headers));
    }

    private MCPServerHttpConfig sscConfig(String url) {
        var config = new MCPServerHttpConfig();
        var sscConfig = new MCPServerHttpConfig.SscConfig();
        sscConfig.setUrl(url);
        config.setSsc(sscConfig);
        return config;
    }

    private MCPServerHttpConfig fodConfig(String url) {
        var config = new MCPServerHttpConfig();
        var fodConfig = new MCPServerHttpConfig.FoDConfig();
        fodConfig.setUrl(url);
        config.setFod(fodConfig);
        return config;
    }
}