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
package com.fortify.cli.common.rest.unirest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class RemoteUrlAuthHelperTest {
    @Test
    public void testParseBasicAuthFromUserInfo() throws Exception {
        var parsed = RemoteUrlAuthHelper.parse("https://user:pa%2Bss%2F%3D@example.com/path?x=1");

        assertEquals("https://example.com/path?x=1", parsed.getRequestUrl());
        assertEquals("Basic "+base64("user:pa+ss/="), parsed.getHeaders().get("Authorization"));
        assertEquals(1, parsed.getHeaders().size());
    }

    @Test
    public void testParseBearerAuthFromUserInfo() throws Exception {
        var parsed = RemoteUrlAuthHelper.parse("https://bearer:abc%2B%2F%3D@example.com/path");

        assertEquals("https://example.com/path", parsed.getRequestUrl());
        assertEquals("Bearer abc+/=", parsed.getHeaders().get("Authorization"));
    }

    @Test
    public void testParseCustomHeadersFromUserInfo() throws Exception {
        var parsed = RemoteUrlAuthHelper.parse(
            "https://headers:Authorization=Bearer%20abc&X-Api-Key=123%2B456@example.com/path"
        );

        assertEquals("https://example.com/path", parsed.getRequestUrl());
        assertEquals("Bearer abc", parsed.getHeaders().get("Authorization"));
        assertEquals("123+456", parsed.getHeaders().get("X-Api-Key"));
        assertEquals(2, parsed.getHeaders().size());
    }

    @Test
    public void testParseNoUserInfoPreservesPercentEncodedPath() throws Exception {
        // Regression test: URLs with percent-encoded characters in the path (e.g. Adoptium JRE
        // download URLs like .../jdk-17.0.9%2B9/...) must not be double-encoded to %252B.
        var url = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/OpenJDK17U-jre_x64_linux_hotspot_17.0.9_9.tar.gz";
        var parsed = RemoteUrlAuthHelper.parse(url);

        assertEquals(url, parsed.getRequestUrl());
        assertTrue(parsed.getHeaders().isEmpty());
    }

    @Test
    public void testParseUserInfoPreservesPercentEncodedPath() throws Exception {
        // Regression test: stripping userinfo from URLs with percent-encoded path must not
        // double-encode those sequences (%2B → %252B).
        var parsed = RemoteUrlAuthHelper.parse(
            "https://user:pass@github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/file.tar.gz"
        );

        assertEquals("https://github.com/adoptium/temurin17-binaries/releases/download/jdk-17.0.9%2B9/file.tar.gz", parsed.getRequestUrl());
    }

    @Test
    public void testParseFileUrlUnchanged() throws Exception {
        var parsed = RemoteUrlAuthHelper.parse("file:/tmp/action.yaml");

        assertEquals("file:/tmp/action.yaml", parsed.getRequestUrl());
        assertTrue(parsed.getHeaders().isEmpty());
    }

    @Test
    public void testOpenStreamUsesUnirestForHttpUrls() throws Exception {
        var receivedAuthHeader = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/action.yaml", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                receivedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
                var body = "name: test-action\n";
                exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
                try ( var os = exchange.getResponseBody() ) {
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                }
            }
        });
        server.start();
        try {
            var url = "http://bearer:abc123@127.0.0.1:"+server.getAddress().getPort()+"/action.yaml";
            try ( InputStream is = RemoteUrlAuthHelper.openStream(url) ) {
                assertEquals("name: test-action\n", new String(is.readAllBytes(), StandardCharsets.UTF_8));
            }
            assertNotNull(receivedAuthHeader.get());
            assertEquals("Bearer abc123", receivedAuthHeader.get());
        } finally {
            server.stop(0);
        }
    }

    private static String base64(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
