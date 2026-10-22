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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.AviatorConnectionPlan;
import com.fortify.cli.aviator.grpc.AviatorGrpcClientHelper.ParsedTarget;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

class AviatorConnectionContextTest {
    private static final String PROXY_PASSWORD = "sentinel-proxy-password";

    @Test
    void requestPolicyFlags() {
        assertFalse(AviatorRequestPolicy.ORDINARY.isWaitForReady());
        assertTrue(AviatorRequestPolicy.ORDINARY.isClassifyConnectivityFailures());
        assertTrue(AviatorRequestPolicy.UNCHANGED.isWaitForReady());
        assertFalse(AviatorRequestPolicy.UNCHANGED.isClassifyConnectivityFailures());
    }

    @Test
    void createsDirectContextFromPlan() {
        var context = AviatorConnectionContext.of(plan(Optional.empty()));

        assertFalse(context.isProxied());
        assertNull(context.proxy());
        assertEquals("aviator.example.com:443", context.firstHop().display());
        assertEquals("direct", context.route());
    }

    @Test
    void createsProxiedContextWithoutCredentials() {
        var proxy = ProxyDescriptor.builder()
            .proxyHost("localhost").proxyPort(33444)
            .proxyUser("sentinel-user").proxyPassword(PROXY_PASSWORD.toCharArray())
            .build();

        var context = AviatorConnectionContext.of(plan(Optional.of(proxy)));

        assertTrue(context.isProxied());
        assertEquals("localhost:33444", context.firstHop().display());
        assertEquals("proxy", context.route());
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

    @ParameterizedTest
    @EnumSource(AviatorConnectivityFailureKind.class)
    void messagesHaveSummaryAndGuidanceWithoutTrailingPeriod(AviatorConnectivityFailureKind kind) {
        var context = new AviatorConnectionContext(new AviatorEndpoint("aviator.example.com", 443), new AviatorEndpoint("localhost", 33444));
        var lines = kind.formatMessage(context).split("\n");

        assertEquals(2, lines.length);
        assertFalse(lines[0].contains("%s"));
        assertFalse(lines[0].endsWith("."));
        assertFalse(lines[1].endsWith("."));
    }

    @Test
    void proxyRefusedMessageNamesTheProxy() {
        var context = new AviatorConnectionContext(new AviatorEndpoint("aviator.example.com", 443), new AviatorEndpoint("localhost", 33444));

        assertEquals("Connection to proxy localhost:33444 was refused\n"
                + "Check that the proxy is running and that its host and port are correct",
            AviatorConnectivityFailureKind.PROXY_REFUSED.formatMessage(context));
    }

    private static AviatorConnectionPlan plan(Optional<ProxyDescriptor> proxy) {
        return new AviatorConnectionPlan("aviator.example.com", "https://aviator.example.com",
            new ParsedTarget("aviator.example.com", null), 443, proxy);
    }
}
