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
package com.fortify.cli.util.mcp_server.helper.http;

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
        var config = new MCPServerHttpConfig();
        var sscConfig = new MCPServerHttpConfig.SscConfig();
        sscConfig.setUrl("https://ssc.example.com");
        config.setSsc(sscConfig);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var cacheKey = resolver.createAuthCacheKey(transportContext(Map.of(
                MCPServerHttpSessionDescriptorResolver.HEADER_SSC_TOKEN, List.of("ssc-token"),
                MCPServerHttpSessionDescriptorResolver.HEADER_SC_SAST_CLIENT_AUTH_TOKEN, List.of("sast-token")
        )));

        assertTrue(cacheKey.startsWith("ssc|"));
        assertFalse(cacheKey.contains("ssc-token"));
        assertFalse(cacheKey.contains("sast-token"));
    }

    @Test
    void createAuthCacheKeyHashesFoDClientCredentials() {
        var config = new MCPServerHttpConfig();
        var fodConfig = new MCPServerHttpConfig.FoDConfig();
        fodConfig.setUrl("https://api.ams.fortify.com");
        config.setFod(fodConfig);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var cacheKey = resolver.createAuthCacheKey(transportContext(Map.of(
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_CLIENT_ID, List.of("client-id"),
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_CLIENT_SECRET, List.of("client-secret")
        )));

        assertTrue(cacheKey.startsWith("fod-client|"));
        assertFalse(cacheKey.contains("client-id"));
        assertFalse(cacheKey.contains("client-secret"));
    }

    @Test
    void createAuthCacheKeyRejectsMixedFoDAuthModes() {
        var config = new MCPServerHttpConfig();
        var fodConfig = new MCPServerHttpConfig.FoDConfig();
        fodConfig.setUrl("https://api.ams.fortify.com");
        config.setFod(fodConfig);
        var resolver = new MCPServerHttpSessionDescriptorResolver(config);

        var exception = assertThrows(FcliSimpleException.class, () -> resolver.createAuthCacheKey(transportContext(Map.of(
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_CLIENT_ID, List.of("client-id"),
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_CLIENT_SECRET, List.of("client-secret"),
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_TENANT, List.of("tenant"),
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_USER, List.of("user"),
                MCPServerHttpSessionDescriptorResolver.HEADER_FOD_PAT, List.of("pat")
        ))));

        assertTrue(exception.getMessage().contains("Specify either FoD client headers"));
    }

    private McpTransportContext transportContext(Map<String, List<String>> headers) {
        return McpTransportContext.create(Map.of("headers", headers));
    }
}