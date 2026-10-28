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
package com.fortify.cli.aviator.grpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

class AviatorConnectionContextTest {
    private static final String PROXY_PASSWORD = "sentinel-proxy-password";

    @Test
    void createsDirectContextFromPlan() {
        var context = AviatorConnectionContext.of(plan(Optional.empty()));

        assertFalse(context.isProxied());
        assertNull(context.proxy());
        assertEquals("aviator.example.com:443", context.target().display());
    }

    @Test
    void createsProxiedContextWithoutCredentials() {
        var proxy = ProxyDescriptor.builder()
            .proxyHost("localhost").proxyPort(33444)
            .proxyUser("sentinel-user").proxyPassword(PROXY_PASSWORD.toCharArray())
            .build();

        var context = AviatorConnectionContext.of(plan(Optional.of(proxy)));

        assertTrue(context.isProxied());
        assertEquals("localhost:33444", context.proxy().display());
        assertFalse(context.toString().contains(PROXY_PASSWORD));
        assertFalse(context.toString().contains("sentinel-user"));
    }

    @Test
    void requiresTarget() {
        assertThrows(NullPointerException.class, () -> new AviatorConnectionContext(null, null));
    }

    @Test
    void replacesControlCharactersInHostNames() {
        assertEquals("evil?host:1", new AviatorEndpoint("evil\u001bhost", 1).display());
        assertEquals("unknown:1", new AviatorEndpoint(null, 1).display());
    }

    @Test
    void displaysIpv6Hosts() {
        assertEquals("[::1]:443", new AviatorEndpoint("::1", 443).display());
        assertEquals("[::1]:443", new AviatorEndpoint("[::1]", 443).display());
    }

    private static AviatorConnectionPlan plan(Optional<ProxyDescriptor> proxy) {
        return new AviatorConnectionPlan("aviator.example.com", "https://aviator.example.com",
            new ParsedTarget("aviator.example.com", null), 443, proxy);
    }
}
