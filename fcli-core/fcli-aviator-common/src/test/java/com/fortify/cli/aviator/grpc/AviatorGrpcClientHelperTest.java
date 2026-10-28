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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.UnsupportedAviatorUrlSchemeException;
import com.fortify.cli.common.http.proxy.helper.ProxyDescriptor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.grpc.HttpConnectProxiedSocketAddress;

class AviatorGrpcClientHelperTest {
    private static final List<String> URL_MARKERS =
        List.of("sentinel-userinfo", "sentinel-path", "sentinel-query", "sentinel-fragment");

    @Test
    void shouldOmitIgnoredUrlDetailsFromWarnings() {
        var logger = (Logger)LoggerFactory.getLogger(AviatorGrpcClientHelper.class);
        var previousLevel = logger.getLevel();
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        try {
            var target = AviatorGrpcClientHelper.parseTarget(
                "https://user:sentinel-userinfo@aviator.invalid:8443/sentinel-path?token=sentinel-query#sentinel-fragment");

            assertEquals("aviator.invalid", target.host());
            assertEquals(8443, target.port());
            assertEquals(1, appender.list.size());
            var event = appender.list.get(0);
            assertEquals(Level.WARN, event.getLevel());
            assertTrue(event.getFormattedMessage().contains("Ignoring path"));
            URL_MARKERS.forEach(marker -> assertFalse(event.getFormattedMessage().contains(marker), marker));
            assertNull(event.getThrowableProxy());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://user:sentinel-userinfo@aviator.invalid:99999/sentinel-path?token=sentinel-query#sentinel-fragment",
        "https://user:sentinel-userinfo@aviator.invalid/sentinel-path/%zz?token=sentinel-query#sentinel-fragment",
        "http://user:sentinel-userinfo@aviator.invalid/sentinel-path?token=sentinel-query#sentinel-fragment"
    })
    void shouldOmitUrlDetailsFromInvalidTargetMessages(String url) {
        var exception = assertThrows(AviatorSimpleException.class, () -> AviatorGrpcClientHelper.parseTarget(url));

        URL_MARKERS.forEach(marker -> assertFalse(exception.getMessage().contains(marker), marker));
        assertNull(exception.getCause());
    }

    @Test
    void shouldParseSchemeLessHostAndPort() {
        var parsed = AviatorGrpcClientHelper.parseTarget("aviator.invalid:443");

        assertEquals("aviator.invalid", parsed.host());
        assertEquals(443, parsed.port());
    }

    @Test
    void shouldParseTargetWithoutPort() {
        var parsed = AviatorGrpcClientHelper.parseTarget("aviator.invalid");

        assertEquals("aviator.invalid", parsed.host());
        assertNull(parsed.port());
    }

    @Test
    void shouldCreateConnectionPlanWithDefaultPortAndNormalizedUrl() {
        var plan = AviatorGrpcClientHelper.createConnectionPlan("aviator.invalid");

        assertEquals("aviator.invalid", plan.originalUrl());
        assertEquals("https://aviator.invalid", plan.normalizedUrl());
        assertEquals("aviator.invalid", plan.target().host());
        assertNull(plan.target().port());
        assertEquals(443, plan.effectivePort());
    }

    @Test
    void shouldCreateConnectionPlanWithExplicitPort() {
        var plan = AviatorGrpcClientHelper.createConnectionPlan("https://aviator.invalid:8443/");

        assertEquals("https://aviator.invalid:8443/", plan.originalUrl());
        assertEquals("https://aviator.invalid:8443/", plan.normalizedUrl());
        assertEquals("aviator.invalid", plan.target().host());
        assertEquals(8443, plan.target().port());
        assertEquals(8443, plan.effectivePort());
    }

    @Test
    void shouldCanonicalizeHttpsSchemeCasing() {
        assertEquals("https://aviator.invalid", AviatorGrpcClientHelper.normalizeUrl("HTTPS://aviator.invalid"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "mttp://vacant",
        "ftp://aviator.invalid",
        "http://aviator.invalid",
        "grpc://aviator.invalid"
    })
    void shouldRejectNonHttpsSchemes(String url) {
        var ex = assertThrows(UnsupportedAviatorUrlSchemeException.class,
            () -> AviatorGrpcClientHelper.createConnectionPlan(url));
        assertEquals(url.substring(0, url.indexOf("://")), ex.getScheme());
        assertEquals(url, ex.getProvidedUrl());
        assertTrue(ex.getMessage().contains(UnsupportedAviatorUrlSchemeException.STAGE_SUMMARY));
    }

    @Test
    void shouldBuildHttpConnectProxyAddressWithCredentials() {
        var proxy = ProxyDescriptor.builder()
            .proxyHost("localhost")
            .proxyPort(8443)
            .proxyUser("user")
            .proxyPassword("pwd".toCharArray())
            .build();
        var target = new InetSocketAddress("aviator.invalid", 443);

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
        var proxy = ProxyDescriptor.builder().proxyHost("proxy.invalid").proxyPort(8443).build();
        SocketAddress unsupportedTarget = new SocketAddress() { private static final long serialVersionUID = 1L; };

        var proxied = AviatorGrpcClientHelper.toProxiedSocketAddress(unsupportedTarget, proxy);

        assertNull(proxied);
    }

    @ParameterizedTest
    @CsvSource({
        "https://user:sentinel-secret@aviator.invalid:443/path, https://***@aviator.invalid:443",
        "user:sentinel-secret@aviator.invalid:443, ***@aviator.invalid:443",
        "mttp://sentinel-secret@aviator.invalid, mttp://***@aviator.invalid",
        "https://aviator.invalid:443/a@b, https://aviator.invalid:443",
        "https://aviator.invalid?token=sentinel-query#sentinel-fragment, https://aviator.invalid",
        "aviator.invalid:443, aviator.invalid:443"
    })
    void shouldRemoveUserInfoFromDisplayedUrl(String url, String expected) {
        assertEquals(expected, AviatorGrpcClientHelper.toSafeUrlForDisplay(url));
    }

    @Test
    void shouldNotExposeUserInfoInInvalidPortMessage() {
        var ex = assertThrows(AviatorSimpleException.class,
            () -> AviatorGrpcClientHelper.parseTarget("https://user:sentinel-secret@aviator.invalid:99999"));

        assertFalse(ex.getMessage().contains("sentinel-secret"));
        assertTrue(ex.getMessage().contains("***@aviator.invalid:99999"));
    }

    @Test
    void shouldNotExposeUserInfoInUnsupportedSchemeException() {
        var ex = assertThrows(UnsupportedAviatorUrlSchemeException.class,
            () -> AviatorGrpcClientHelper.parseTarget("http://user:sentinel-secret@aviator.invalid"));

        assertFalse(ex.getMessage().contains("sentinel-secret"));
        assertFalse(ex.getProvidedUrl().contains("sentinel-secret"));
    }
}
