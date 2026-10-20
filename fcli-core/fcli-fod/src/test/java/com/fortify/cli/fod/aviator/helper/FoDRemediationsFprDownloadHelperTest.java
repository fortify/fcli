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
package com.fortify.cli.fod.aviator.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

class FoDRemediationsFprDownloadHelperTest {
    private static final byte[] FPR_BYTES = {'P', 'K', 3, 4, 20, 0, 0, 0, 8, 0, (byte) 0x9c, 0, 0, 0};
    private static final byte[] OLD_CONTENT = "previous content".getBytes(StandardCharsets.UTF_8);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(20);

    @TempDir Path tempDir;
    private HttpServer server;
    private UnirestInstance unirest;
    private final AtomicInteger requestCount = new AtomicInteger();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ready", ex -> respond(ex, 200, FPR_BYTES));
        server.createContext("/ready-after-2", ex -> respond(ex, requestCount.get() < 2 ? 202 : 200,
                requestCount.get() < 2 ? new byte[0] : FPR_BYTES));
        server.createContext("/always-accepted", ex -> respond(ex, 202, new byte[0]));
        server.createContext("/error", ex -> respond(ex, 500, "{\"error\":\"failed\"}".getBytes(StandardCharsets.UTF_8)));
        server.createContext("/partial", this::respondPartial);
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
    void pollingPolicyIsTenAttemptsThirtySecondsApart() {
        assertEquals(10, FoDRemediationsFprDownloadHelper.MAX_ATTEMPTS);
        assertEquals(Duration.ofSeconds(30), FoDRemediationsFprDownloadHelper.POLL_INTERVAL);
    }

    @Test
    void downloadsToLocalFile() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        download("/ready", dest, 3);
        assertArrayEquals(FPR_BYTES, Files.readAllBytes(dest));
        assertEquals(1, requestCount.get());
        assertNoTempFiles(tempDir);
    }

    @Test
    void pollsOnAcceptedUntilReady() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        download("/ready-after-2", dest, 5);
        assertEquals(3, requestCount.get());
        assertArrayEquals(FPR_BYTES, Files.readAllBytes(dest));
    }

    @Test
    void acceptedResponseIsNotTreatedAsCompletedDownload() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        var ex = assertThrows(FcliSimpleException.class, () -> download("/always-accepted", dest, 3));
        assertTrue(ex.getMessage().contains("3 attempts"), ex.getMessage());
        assertEquals(3, requestCount.get());
        assertFalse(Files.exists(dest));
        assertNoTempFiles(tempDir);
    }

    @Test
    void timeoutKeepsExistingLocalFile() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        Files.write(dest, OLD_CONTENT);
        assertThrows(FcliSimpleException.class, () -> download("/always-accepted", dest, 2));
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles(tempDir);
    }

    @Test
    void httpErrorKeepsExistingLocalFile() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        Files.write(dest, OLD_CONTENT);
        assertThrows(UnexpectedHttpResponseException.class, () -> download("/error", dest, 3));
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles(tempDir);
    }

    @Test
    void interruptedTransferKeepsExistingLocalFile() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        Files.write(dest, OLD_CONTENT);
        assertTransferFailure(() -> download("/partial", dest, 3));
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles(tempDir);
    }

    @Test
    void interruptionWhilePollingFailsKeepsExistingFileAndPreservesInterruptFlag() throws Exception {
        var dest = tempDir.resolve("release.fpr");
        Files.write(dest, OLD_CONTENT);
        var failure = new AtomicReference<Throwable>();
        var interruptFlagPreserved = new AtomicBoolean();
        var worker = new Thread(() -> {
            try {
                FoDRemediationsFprDownloadHelper.download(request("/always-accepted"), dest, Duration.ofSeconds(30), 3);
            } catch (Throwable t) {
                failure.set(t);
                interruptFlagPreserved.set(Thread.currentThread().isInterrupted());
            }
        });
        worker.start();
        long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        while (requestCount.get() < 1 && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        worker.interrupt();
        worker.join(10_000);

        assertFalse(worker.isAlive(), "Download must stop promptly on interruption");
        assertTrue(failure.get() instanceof FcliTechnicalException, String.valueOf(failure.get()));
        assertTrue(interruptFlagPreserved.get(), "Interrupt flag must be preserved");
        assertEquals(1, requestCount.get());
        assertArrayEquals(OLD_CONTENT, Files.readAllBytes(dest));
        assertNoTempFiles(tempDir);
    }

    @Test
    void downloadsIntoZipEntry() throws Exception {
        try (FileSystem zipFs = newZipFileSystem()) {
            var entry = zipFs.getPath("fprs", "release.fpr");
            Files.createDirectories(entry.getParent());
            download("/ready-after-2", entry, 5);
            assertArrayEquals(FPR_BYTES, Files.readAllBytes(entry));
            assertEquals(3, requestCount.get());
            assertNoTempFiles(entry.getParent());
        }
    }

    @Test
    void zipEntryIsReplacedOnlyAfterSuccessfulDownload() throws Exception {
        try (FileSystem zipFs = newZipFileSystem()) {
            var entry = zipFs.getPath("fprs", "release.fpr");
            Files.createDirectories(entry.getParent());
            Files.write(entry, OLD_CONTENT);
            assertThrows(FcliSimpleException.class, () -> download("/always-accepted", entry, 2));
            assertThrows(UnexpectedHttpResponseException.class, () -> download("/error", entry, 2));
            assertTransferFailure(() -> download("/partial", entry, 2));
            assertArrayEquals(OLD_CONTENT, Files.readAllBytes(entry));
            assertNoTempFiles(entry.getParent());
        }
    }

    @Test
    void failedZipDownloadLeavesNoLocalTempFiles() throws Exception {
        var before = countLocalTempFiles();
        try (FileSystem zipFs = newZipFileSystem()) {
            var entry = zipFs.getPath("release.fpr");
            assertThrows(UnexpectedHttpResponseException.class, () -> download("/error", entry, 2));
        }
        assertEquals(before, countLocalTempFiles());
    }

    private static void assertTransferFailure(Executable download) {
        var e = assertThrows(RuntimeException.class, download);
        assertTrue(e instanceof FcliTechnicalException || e instanceof UnexpectedHttpResponseException, e.toString());
    }

    private void download(String path, Path destination, int maxAttempts) {
        FoDRemediationsFprDownloadHelper.download(request(path), destination, POLL_INTERVAL, maxAttempts);
    }

    private GetRequest request(String path) {
        return unirest.get("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    private FileSystem newZipFileSystem() throws IOException {
        return FileSystems.newFileSystem(tempDir.resolve("cache.zip"), Map.of("create", "true"));
    }

    private static long countLocalTempFiles() throws IOException {
        try (var files = Files.list(Path.of(System.getProperty("java.io.tmpdir")))) {
            return files.filter(p -> p.getFileName().toString().startsWith("fcli-fod-fpr-")).count();
        }
    }

    private static void assertNoTempFiles(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().endsWith(".part")), "Temporary files left behind");
        }
    }

    private void respond(HttpExchange exchange, int status, byte[] body) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", status == 500 ? "application/json" : "application/octet-stream");
        exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void respondPartial(HttpExchange exchange) throws IOException {
        requestCount.incrementAndGet();
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, 100_000);
        var os = exchange.getResponseBody();
        os.write(FPR_BYTES);
        os.flush();
        throw new IOException("Simulated connection abort");
    }
}
