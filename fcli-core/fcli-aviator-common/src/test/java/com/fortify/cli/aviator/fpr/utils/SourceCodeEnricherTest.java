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
package com.fortify.cli.aviator.fpr.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.util.FprHandle;

class SourceCodeEnricherTest {

    @TempDir
    Path tempDir;

    @Test
    void reportsEveryUniqueDecodeFailureAndDoesNotHideSuccessfulFiles() throws Exception {
        Map<String, byte[]> sourceFiles = new LinkedHashMap<>();
        sourceFiles.put("good.java", "class Good {}".getBytes(StandardCharsets.UTF_8));
        sourceFiles.put("bad-one.java", new byte[] {(byte) 0xFF});
        sourceFiles.put("bad-two.java", new byte[] {(byte) 0xFE});
        Path fprPath = createFpr(sourceFiles);

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            SourceCodeEnricher.EnrichmentResult result = new SourceCodeEnricher(
                    fprHandle, SourceDecoders.fromToken("UTF-8"), null)
                    .enrichWithSourceCodeDetailed(List.of(List.of(
                            element("good.java"),
                            element("bad-one.java"),
                            element("bad-two.java"),
                            element("bad-one.java"))));

            assertTrue(result.hasFailures());
            assertEquals(List.of("bad-one.java", "bad-two.java"),
                    result.failures().stream().map(SourceCodeEnricher.SourceFileFailure::filename).toList());
            assertEquals(List.of(AuditSkipReason.SOURCE_FILE_DECODE_FAILED, AuditSkipReason.SOURCE_FILE_DECODE_FAILED),
                    result.failures().stream().map(SourceCodeEnricher.SourceFileFailure::reason).toList());
            assertEquals(List.of("good.java"), result.files().keySet().stream().toList());
        }
    }

    @Test
    void classifiesReadFailuresSeparatelyFromDecodeFailures() throws Exception {
        Path fprPath = createFprWithMissingSource("missing.java");

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            SourceCodeEnricher.EnrichmentResult result = new SourceCodeEnricher(
                    fprHandle, SourceDecoders.fromToken("UTF-8"), null)
                    .enrichWithSourceCodeDetailed(List.of(List.of(element("missing.java"))));

            assertEquals(AuditSkipReason.SOURCE_FILE_READ_FAILED, result.failures().get(0).reason());
        }
    }

    @Test
    void cachesSuccessfulAndFailedLoadsAcrossEnrichmentCalls() throws Exception {
        Map<String, byte[]> sourceFiles = new LinkedHashMap<>();
        sourceFiles.put("good.java", "class Good {}".getBytes(StandardCharsets.UTF_8));
        sourceFiles.put("bad.java", new byte[] {(byte) 0xFF});
        Path fprPath = createFpr(sourceFiles);
        AtomicInteger decodeCalls = new AtomicInteger();
        ISourceDecoder decoder = new ISourceDecoder() {
            @Override
            public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata metadata) {
                decodeCalls.incrementAndGet();
                if (bytes.length > 0 && bytes[0] == (byte) 0xFF) {
                    throw new SourceDecodeException("decode failed");
                }
                return new DecodeResult(new String(bytes, StandardCharsets.UTF_8), StandardCharsets.UTF_8, "test");
            }

            @Override
            public String describe() {
                return "test";
            }
        };

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            SourceCodeEnricher enricher = new SourceCodeEnricher(fprHandle, decoder, null);
            List<List<StackTraceElement>> stackTraces = List.of(List.of(element("good.java"), element("bad.java")));

            SourceCodeEnricher.EnrichmentResult firstResult = enricher.enrichWithSourceCodeDetailed(stackTraces);
            SourceCodeEnricher.EnrichmentResult secondResult = enricher.enrichWithSourceCodeDetailed(stackTraces);

            assertEquals(2, decodeCalls.get());
            assertEquals(List.of("good.java"), secondResult.files().keySet().stream().toList());
            assertEquals(List.of("bad.java"), secondResult.failures().stream()
                    .map(SourceCodeEnricher.SourceFileFailure::filename).toList());
            assertEquals(firstResult.files().get("good.java").getContent(), secondResult.files().get("good.java").getContent());
        }
    }

    @Test
    void loadsAFileOnceWhenEnrichmentCallsAreConcurrent() throws Exception {
        Map<String, byte[]> sourceFiles = Map.of("good.java", "class Good {}".getBytes(StandardCharsets.UTF_8));
        Path fprPath = createFpr(sourceFiles);
        AtomicInteger decodeCalls = new AtomicInteger();
        CountDownLatch decodeStarted = new CountDownLatch(1);
        CountDownLatch allowDecode = new CountDownLatch(1);
        ISourceDecoder decoder = new ISourceDecoder() {
            @Override
            public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata metadata) {
                decodeCalls.incrementAndGet();
                decodeStarted.countDown();
                try {
                    if (!allowDecode.await(5, TimeUnit.SECONDS)) {
                        throw new SourceDecodeException("Timed out waiting to decode");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SourceDecodeException("Interrupted while decoding", e);
                }
                return new DecodeResult(new String(bytes, StandardCharsets.UTF_8), StandardCharsets.UTF_8, "test");
            }

            @Override
            public String describe() {
                return "test";
            }
        };

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
            SourceCodeEnricher enricher = new SourceCodeEnricher(fprHandle, decoder, null);
            List<List<StackTraceElement>> stackTraces = List.of(List.of(element("good.java")));
            Future<SourceCodeEnricher.EnrichmentResult> first = executor.submit(
                    () -> enricher.enrichWithSourceCodeDetailed(stackTraces));
            Future<SourceCodeEnricher.EnrichmentResult> second = executor.submit(
                    () -> enricher.enrichWithSourceCodeDetailed(stackTraces));

            assertTrue(decodeStarted.await(5, TimeUnit.SECONDS));
            allowDecode.countDown();

            assertEquals(List.of("good.java"), first.get(5, TimeUnit.SECONDS).files().keySet().stream().toList());
            assertEquals(List.of("good.java"), second.get(5, TimeUnit.SECONDS).files().keySet().stream().toList());
            assertEquals(1, decodeCalls.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    void cachesConcurrentDecodeFailureOnce() throws Exception {
        Map<String, byte[]> sourceFiles = Map.of("bad.java", new byte[] {(byte) 0xFF});
        Path fprPath = createFpr(sourceFiles);
        AtomicInteger decodeCalls = new AtomicInteger();
        CountDownLatch decodeStarted = new CountDownLatch(1);
        CountDownLatch allowDecode = new CountDownLatch(1);
        ISourceDecoder decoder = new ISourceDecoder() {
            @Override
            public DecodeResult decode(byte[] bytes, String filename, FVDLMetadata metadata) {
                decodeCalls.incrementAndGet();
                decodeStarted.countDown();
                try {
                    if (!allowDecode.await(5, TimeUnit.SECONDS)) {
                        throw new SourceDecodeException("Timed out waiting to decode");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new SourceDecodeException("Interrupted while decoding", e);
                }
                throw new SourceDecodeException("decode failed");
            }

            @Override
            public String describe() {
                return "test";
            }
        };

        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                SourceCodeEnricher enricher = new SourceCodeEnricher(fprHandle, decoder, null);
                List<List<StackTraceElement>> stackTraces = List.of(List.of(element("bad.java")));
                Future<SourceCodeEnricher.EnrichmentResult> first = executor.submit(
                        () -> enricher.enrichWithSourceCodeDetailed(stackTraces));
                Future<SourceCodeEnricher.EnrichmentResult> second = executor.submit(
                        () -> enricher.enrichWithSourceCodeDetailed(stackTraces));

                assertTrue(decodeStarted.await(5, TimeUnit.SECONDS));
                allowDecode.countDown();

                assertEquals(List.of("bad.java"), first.get(5, TimeUnit.SECONDS).failures().stream()
                        .map(SourceCodeEnricher.SourceFileFailure::filename).toList());
                assertEquals(List.of("bad.java"), second.get(5, TimeUnit.SECONDS).failures().stream()
                        .map(SourceCodeEnricher.SourceFileFailure::filename).toList());
                assertEquals(1, decodeCalls.get());
            } finally {
                executor.shutdownNow();
            }
        }
    }

    private static StackTraceElement element(String filename) {
        return new StackTraceElement(filename, 1, "", "", null, null, null);
    }

    private Path createFpr(Map<String, byte[]> sourceFiles) throws IOException {
        Path fprPath = tempDir.resolve("source-enricher-test.fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zip, "src-archive/index.xml", createIndex(sourceFiles.keySet()).getBytes(StandardCharsets.UTF_8));
            int index = 1;
            for (Map.Entry<String, byte[]> sourceFile : sourceFiles.entrySet()) {
                writeEntry(zip, "src-archive/" + index++, sourceFile.getValue());
            }
        }
        return fprPath;
    }

    private Path createFprWithMissingSource(String filename) throws IOException {
        Path fprPath = tempDir.resolve("source-enricher-missing-source.fpr");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(fprPath))) {
            writeEntry(zip, "src-archive/index.xml", createIndex(List.of(filename)).getBytes(StandardCharsets.UTF_8));
        }
        return fprPath;
    }

    private static String createIndex(Iterable<String> filenames) {
        StringBuilder index = new StringBuilder("<?xml version='1.0' encoding='utf8'?><properties>");
        int entry = 1;
        for (String filename : filenames) {
            index.append("<entry key=\"").append(filename).append("\">src-archive/")
                    .append(entry++).append("</entry>");
        }
        return index.append("</properties>").toString();
    }

    private static void writeEntry(ZipOutputStream zip, String name, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(bytes);
        zip.closeEntry();
    }
}