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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

class UnirestHelperDownloadTest {
    private static final byte[] CONTENT = "tool archive".getBytes(StandardCharsets.UTF_8);
    private static final byte[] OLD_CONTENT = "previous".getBytes(StandardCharsets.UTF_8);

    @TempDir Path tempDir;
    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tool.zip", ex -> respond(ex, 200, CONTENT));
        server.createContext("/missing.zip", ex -> respond(ex, 404, "<html>Not Found</html>".getBytes(StandardCharsets.UTF_8)));
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void downloadsFile() throws Exception {
        var dest = tempDir.resolve("tool.zip").toFile();
        var result = UnirestHelper.download("test", url("/tool.zip"), dest);
        assertEquals(dest, result);
        assertArrayEquals(CONTENT, Files.readAllBytes(dest.toPath()));
    }

    @Test
    void failsOnHttpErrorAndKeepsDestination() throws Exception {
        var dest = tempDir.resolve("tool.zip");
        Files.write(dest, OLD_CONTENT);
        var e = assertThrows(UnexpectedHttpResponseException.class,
                () -> UnirestHelper.download("test", url("/missing.zip"), dest.toFile()));
        assertEquals(404, e.getStatus());
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        try ( var files = Files.list(tempDir) ) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().endsWith(".part")));
        }
    }

    private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.sendResponseHeaders(status, body.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(body);
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:"+server.getAddress().getPort()+path;
    }
}
