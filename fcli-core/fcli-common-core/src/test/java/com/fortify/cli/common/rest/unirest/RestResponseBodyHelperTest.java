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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;

class RestResponseBodyHelperTest {
    private static final byte[] ZIP_BYTES = {'P', 'K', 3, 4, 20, 0, 0, 0, 8, 0, (byte)0x9c, 0, 0, 0};
    private static final byte[] OLD_CONTENT = "previous content".getBytes(StandardCharsets.UTF_8);
    private static final int LARGE_SIZE = 64 * 1024 * 1024;

    @TempDir Path tempDir;
    private HttpServer server;
    private UnirestInstance unirest;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/fpr", ex -> respond(ex, 200, "application/octet-stream", ZIP_BYTES));
        server.createContext("/notfound", ex -> respond(ex, 404, "application/json", "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/text", ex -> respond(ex, 200, "text/plain; charset=ISO-8859-1", "café".getBytes(StandardCharsets.ISO_8859_1)));
        server.createContext("/empty", ex -> respond(ex, 200, "text/plain", new byte[0]));
        server.createContext("/partial", this::respondPartial);
        server.createContext("/large", this::respondLarge);
        server.start();
        unirest = UnirestHelper.createUnirestInstance();
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
    }

    @AfterEach
    void tearDown() {
        unirest.close();
        server.stop(0);
    }

    @Test
    void saveToFileWritesExactBytesAndReturnsRecord() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        var result = RestResponseBodyHelper.saveToFile(unirest.get(url("/fpr")), dest, null);

        assertArrayEquals(ZIP_BYTES, Files.readAllBytes(dest));
        assertEquals(dest.toAbsolutePath().normalize(), result.file());
        assertEquals(ZIP_BYTES.length, result.size());
        assertEquals("application/octet-stream", result.contentType());
        assertEquals(200, result.status());
        var node = result.asObjectNode();
        assertEquals(dest.toAbsolutePath().normalize().toString(), node.get("file").asText());
        assertEquals(ZIP_BYTES.length, node.get("size").asLong());
        assertEquals(200, node.get("status").asInt());
        assertNoTempFiles();
    }

    @Test
    void saveToFileReplacesExistingFileOnSuccess() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        Files.write(dest, OLD_CONTENT);
        RestResponseBodyHelper.saveToFile(unirest.get(url("/fpr")), dest, null);
        assertArrayEquals(ZIP_BYTES, Files.readAllBytes(dest));
    }

    @Test
    void saveToFileKeepsExistingFileOnHttpError() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        Files.write(dest, OLD_CONTENT);
        var e = assertThrows(UnexpectedHttpResponseException.class,
                () -> RestResponseBodyHelper.saveToFile(unirest.get(url("/notfound")), dest, null));
        assertEquals(404, e.getStatus());
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles();
    }

    @Test
    void saveToFileFailsOnHttpErrorWithoutInterceptor() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        Files.write(dest, OLD_CONTENT);
        try ( var plainUnirest = UnirestHelper.createUnirestInstance() ) {
            assertThrows(UnexpectedHttpResponseException.class,
                    () -> RestResponseBodyHelper.saveToFile(plainUnirest.get(url("/notfound")), dest, null));
        }
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles();
    }

    @Test
    void saveToFileRejectsMissingParentDirectoryWithoutSendingRequest() {
        var dest = tempDir.resolve("missing").resolve("scan.fpr");
        var e = assertThrows(FcliSimpleException.class,
                () -> RestResponseBodyHelper.saveToFile(unirest.get(url("/fpr")), dest, null));
        assertTrue(e.getMessage().contains("Directory does not exist"), e.getMessage());
        assertEquals(0, requestCount.get());
    }

    @Test
    void saveToFileKeepsExistingFileOnInterruptedTransfer() throws Exception {
        var dest = tempDir.resolve("scan.fpr");
        Files.write(dest, OLD_CONTENT);
        assertThrows(UnirestException.class,
                () -> RestResponseBodyHelper.saveToFile(unirest.get(url("/partial")), dest, null));
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles();
    }

    @Test
    void saveToFileStreamsLargeBodyWithBoundedMemory() throws Exception {
        var dest = tempDir.resolve("large.bin");
        var runtime = Runtime.getRuntime();
        System.gc();
        var usedBefore = runtime.totalMemory() - runtime.freeMemory();
        var result = RestResponseBodyHelper.saveToFile(unirest.get(url("/large")), dest, null);
        System.gc();
        var usedAfter = runtime.totalMemory() - runtime.freeMemory();

        assertEquals(LARGE_SIZE, Files.size(dest));
        assertEquals(LARGE_SIZE, result.size());
        assertTrue(usedAfter - usedBefore < LARGE_SIZE / 2,
                "Heap grew by "+(usedAfter - usedBefore)+" bytes while saving a "+LARGE_SIZE+" byte body");
    }

    @Test
    void asTextDecodesDeclaredCharset() {
        assertEquals("café", RestResponseBodyHelper.asText(unirest.get(url("/text")), "use file mode"));
    }

    @Test
    void asTextReturnsEmptyStringForEmptyBody() {
        assertEquals("", RestResponseBodyHelper.asText(unirest.get(url("/empty")), "use file mode"));
    }

    @Test
    void asTextRejectsBinaryBodyWithGuidance() {
        var e = assertThrows(FcliSimpleException.class,
                () -> RestResponseBodyHelper.asText(unirest.get(url("/fpr")), "use file mode"));
        assertTrue(e.getMessage().contains("application/octet-stream"), e.getMessage());
        assertTrue(e.getMessage().contains("use file mode"), e.getMessage());
    }

    @ParameterizedTest
    @MethodSource("isTextCases")
    void isTextClassifiesBodies(String contentType, byte[] body, boolean expected) {
        assertEquals(expected, RestResponseBodyHelper.isText(contentType, body));
    }

    static Stream<Arguments> isTextCases() {
        var utf8 = "héllo".getBytes(StandardCharsets.UTF_8);
        var latin1 = "héllo".getBytes(StandardCharsets.ISO_8859_1);
        return Stream.of(
            Arguments.of("text/plain", ZIP_BYTES, true),
            Arguments.of("application/xml", ZIP_BYTES, true),
            Arguments.of("application/vnd.foo+json", ZIP_BYTES, true),
            Arguments.of("Application/CSV; charset=UTF-8", ZIP_BYTES, true),
            Arguments.of("application/octet-stream", utf8, true),
            Arguments.of("application/octet-stream", ZIP_BYTES, false),
            Arguments.of("application/octet-stream", new byte[] {'a', 0, 'b'}, false),
            Arguments.of("application/octet-stream", latin1, false),
            Arguments.of("application/octet-stream; charset=ISO-8859-1", latin1, true),
            Arguments.of(null, utf8, true),
            Arguments.of(null, ZIP_BYTES, false),
            Arguments.of(null, new byte[0], true));
    }

    @Test
    void charsetOfFallsBackToUtf8() {
        assertEquals(StandardCharsets.ISO_8859_1, RestResponseBodyHelper.charsetOf("text/plain; charset=\"ISO-8859-1\""));
        assertEquals(StandardCharsets.UTF_8, RestResponseBodyHelper.charsetOf("text/plain; charset=bogus-charset"));
        assertEquals(StandardCharsets.UTF_8, RestResponseBodyHelper.charsetOf("text/plain"));
        assertEquals(StandardCharsets.UTF_8, RestResponseBodyHelper.charsetOf(null));
    }

    @Test
    void recordUsesJsonNullForMissingContentType() {
        var node = new RestResponseFileRecord(tempDir.resolve("x"), 1, null, 200).asObjectNode();
        assertTrue(node.get("contentType").isNull());
        assertNull(new RestResponseFileRecord(tempDir.resolve("x"), 1, null, 200).contentType());
    }

    private void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body.length==0 ? -1 : body.length);
        try ( OutputStream os = exchange.getResponseBody() ) {
            os.write(body);
        }
    }

    private void respondPartial(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, 100_000);
        var os = exchange.getResponseBody();
        os.write(ZIP_BYTES);
        os.flush();
        // Throwing from the handler makes the server drop the connection before the
        // announced number of bytes has been sent
        throw new IOException("Simulated connection abort");
    }

    private void respondLarge(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, LARGE_SIZE);
        var chunk = new byte[1024 * 1024];
        Arrays.fill(chunk, (byte)'x');
        try ( OutputStream os = exchange.getResponseBody() ) {
            for ( int i = 0; i < LARGE_SIZE / chunk.length; i++ ) { os.write(chunk); }
        }
    }

    private String url(String path) {
        return "http://127.0.0.1:"+server.getAddress().getPort()+path;
    }

    private void assertNoTempFiles() throws IOException {
        try ( var files = Files.list(tempDir) ) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().endsWith(".part")), "Temporary files left behind");
        }
    }
}
