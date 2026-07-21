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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

import io.grpc.HttpConnectProxiedSocketAddress;

class AviatorGrpcClientHelperTest {
    @Test
    void shouldParseSchemeLessHostAndPort() {
        var parsed = AviatorGrpcClientHelper.parseTarget("aviator.example.com:443");

        assertEquals("aviator.example.com", parsed.host());
        assertEquals(443, parsed.port());
    }

    @Test
    void shouldParseTargetWithoutPort() {
        var parsed = AviatorGrpcClientHelper.parseTarget("aviator.example.com");

        assertEquals("aviator.example.com", parsed.host());
        assertNull(parsed.port());
    }

    @Test
    void shouldCreateConnectionPlanWithDefaultPortAndNormalizedUrl() {
        var plan = AviatorGrpcClientHelper.createConnectionPlan("aviator.example.com");

        assertEquals("aviator.example.com", plan.originalUrl());
        assertEquals("https://aviator.example.com", plan.normalizedUrl());
        assertEquals("aviator.example.com", plan.target().host());
        assertNull(plan.target().port());
        assertEquals(443, plan.effectivePort());
    }

    @Test
    void shouldCreateConnectionPlanWithExplicitPort() {
        var plan = AviatorGrpcClientHelper.createConnectionPlan("https://aviator.example.com:8443/");

        assertEquals("https://aviator.example.com:8443/", plan.originalUrl());
        assertEquals("https://aviator.example.com:8443/", plan.normalizedUrl());
        assertEquals("aviator.example.com", plan.target().host());
        assertEquals(8443, plan.target().port());
        assertEquals(8443, plan.effectivePort());
    }

    @Test
    void shouldBuildHttpConnectProxyAddressWithCredentials() {
        var proxy = ProxyDescriptor.builder()
            .proxyHost("localhost")
            .proxyPort(8443)
            .proxyUser("user")
            .proxyPassword("pwd".toCharArray())
            .build();
        var target = new InetSocketAddress("aviator.example.com", 443);

        var proxied = AviatorGrpcClientHelper.toProxiedSocketAddress(target, proxy);

        var httpConnect = assertInstanceOf(HttpConnectProxiedSocketAddress.class, proxied);
        var proxyAddress = assertInstanceOf(InetSocketAddress.class, httpConnect.getProxyAddress());
        assertEquals("localhost", proxyAddress.getHostString());
        assertEquals(8443, proxyAddress.getPort());
        assertEquals("user", httpConnect.getUsername());
        assertEquals("pwd", httpConnect.getPassword());
    }

    @Test
    void shouldIgnoreUnsupportedSocketAddressTypes() {
        var proxy = ProxyDescriptor.builder().proxyHost("proxy.example.com").proxyPort(8443).build();
        SocketAddress unsupportedTarget = new SocketAddress() { private static final long serialVersionUID = 1L; };

        var proxied = AviatorGrpcClientHelper.toProxiedSocketAddress(unsupportedTarget, proxy);

        assertNull(proxied);
    }
}
