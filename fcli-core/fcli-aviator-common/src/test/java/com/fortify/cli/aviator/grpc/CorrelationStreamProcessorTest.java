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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.grpc.CorrelationInitResponse;
import com.fortify.aviator.grpc.CorrelationResponse;
import com.fortify.aviator.grpc.CorrelationServerMessage;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.common.progress.helper.IProgressWriter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class CorrelationStreamProcessorTest {

    @Test
    void buildCorrelationWorkItemsPreservesDastSessionUrlEncounterOrder() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        CorrelationStreamProcessor.CorrelationBucketData bucket = createBucket(
            "SQL Injection",
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/z", "https://example.com/a")
        );

        List<?> items = invokeBuildCorrelationWorkItems(processor, List.of(bucket));

        assertEquals(1, items.size());
        assertIterableEquals(
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/a"),
            getDastUrls(items.get(0))
        );
    }

    @Test
    void buildUrlToDastMapPreservesUrlEncounterOrder() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        CorrelationStreamProcessor.CorrelationBucketData bucket = createBucket(
            "SQL Injection",
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/z", "https://example.com/a")
        );

        Map<String, List<DastIssue>> urlMap = invokeBuildUrlToDastMap(processor, List.of(bucket));

        assertIterableEquals(
            List.of("https://example.com/z", "https://example.com/login", "https://example.com/a"),
            urlMap.keySet()
        );
    }

    @Test
    void buildCorrelationWorkItemsSkipsFindingWhenAllPairsWereTried() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        CorrelationStreamProcessor.CorrelationBucketData bucket = createBucket(
            "SQL Injection",
            List.of("https://example.com/login", "https://example.com/admin")
        );
        Map<String, List<DastIssue>> urlMap = invokeBuildUrlToDastMap(processor, List.of(bucket));
        setField(processor, "urlToDastIssues", urlMap);
        setField(processor, "previouslyTriedPairKeys", Set.of(
            "SAST-1::https://example.com/login",
            "SAST-1::https://example.com/admin"
        ));

        List<?> items = invokeBuildCorrelationWorkItems(processor, List.of(bucket));

        assertEquals(0, items.size());
    }

    @Test
    void unknownCorrelationResponseTypeIsWarnedOnce() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        assertSlf4jWarnOnce(() -> invokeOnNext(processor, CorrelationServerMessage.newBuilder().build()),
                "Unknown correlation response type");
    }

    @Test
    void nonOkInitResponseIsWarnedOnce() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        prepareInitializedStream(processor);
        assertSlf4jWarnOnce(() -> invokeHandleInitResponse(processor, true), "Init response status");
    }

    @Test
    void unknownOrCompletedCorrelationRequestIsWarnedOnce() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, null, null, null, 0, 0);
        assertSlf4jWarnOnce(() -> invokeHandleCorrelationResponse(processor), "unknown or completed request");
    }

    @Test
    void aviatorLoggerPrefixesCorrelationWarningsOnce() throws Exception {
        RecordingProgressWriter progress = new RecordingProgressWriter();
        AviatorLoggerImpl logger = new AviatorLoggerImpl(progress);
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);

        invokeOnNext(processor, CorrelationServerMessage.newBuilder().build());
        invokeHandleCorrelationResponse(processor);
        prepareInitializedStream(processor);
        invokeHandleInitResponse(processor, false);

        assertEquals(3, progress.warnings.size());
        assertWarnOnce(progress.warnings.get(0), "Unknown correlation response type");
        assertWarnOnce(progress.warnings.get(1), "unknown or completed request");
        assertWarnOnce(progress.warnings.get(2), "Init response status");
    }

    private CorrelationStreamProcessor.CorrelationBucketData createBucket(String category, List<String> sessionUrls) {
        List<DastIssue> dastIssues = sessionUrls.stream()
            .map(this::createDastIssue)
            .toList();
        Vulnerability vulnerability = Vulnerability.builder().instanceID("SAST-1").build();
        return new CorrelationStreamProcessor.CorrelationBucketData(category, List.of(vulnerability), dastIssues);
    }

    private DastIssue createDastIssue(String sessionUrl) {
        DastIssue issue = new DastIssue();
        issue.setId(sessionUrl);
        issue.setSessionUrl(sessionUrl);
        return issue;
    }

    @SuppressWarnings("unchecked")
    private List<?> invokeBuildCorrelationWorkItems(CorrelationStreamProcessor processor, List<?> mixedBuckets)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod("buildCorrelationWorkItems", List.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(processor, mixedBuckets);
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<DastIssue>> invokeBuildUrlToDastMap(CorrelationStreamProcessor processor, List<?> mixedBuckets)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod("buildUrlToDastMap", List.class);
        method.setAccessible(true);
        return (Map<String, List<DastIssue>>) method.invoke(processor, mixedBuckets);
    }

    @SuppressWarnings("unchecked")
    private List<String> getDastUrls(Object workItem) throws Exception {
        Method method = workItem.getClass().getDeclaredMethod("dastUrls");
        method.setAccessible(true);
        return (List<String>) method.invoke(workItem);
    }

    private void setField(CorrelationStreamProcessor processor, String fieldName, Object value) throws Exception {
        Field field = CorrelationStreamProcessor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(processor, value);
    }

    private void assertSlf4jWarnOnce(ThrowingRunnable action, String distinguishingText) throws Exception {
        Logger logger = (Logger) LoggerFactory.getLogger(CorrelationStreamProcessor.class);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        try {
            action.run();
            assertEquals(1, appender.list.size());
            assertWarnOnce(appender.list.get(0).getFormattedMessage(), distinguishingText);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
            appender.stop();
        }
    }

    private static void assertWarnOnce(String message, String distinguishingText) {
        assertTrue(message.contains(distinguishingText), message);
        assertFalse(message.contains("WARN: WARN:"), message);
        int count = 0;
        int from = 0;
        while ((from = message.indexOf("WARN:", from)) >= 0) {
            count++;
            from += "WARN:".length();
        }
        assertEquals(1, count, message);
    }

    private void prepareInitializedStream(CorrelationStreamProcessor processor) throws Exception {
        setField(processor, "state", new CorrelationStreamState("stream-1",
                new CorrelationStreamConfig("token", "app", null, null, null)));
        setField(processor, "initializationTimeout", new StreamInitializationTimeout(null, () -> { }));
        setField(processor, "correlationWorkItems", List.of());
    }

    private void invokeOnNext(CorrelationStreamProcessor processor, CorrelationServerMessage message) throws Exception {
        Class<?> type = Class.forName(
                "com.fortify.cli.aviator.grpc.CorrelationStreamProcessor$CorrelationResponseObserver");
        Constructor<?> constructor = type.getDeclaredConstructor(
                CorrelationStreamProcessor.class, CompletableFuture.class, String.class);
        constructor.setAccessible(true);
        Object observer = constructor.newInstance(processor, new CompletableFuture<CorrelationResult>(), "scan");
        Method onNext = type.getDeclaredMethod("onNext", CorrelationServerMessage.class);
        onNext.setAccessible(true);
        onNext.invoke(observer, message);
    }

    private void invokeHandleInitResponse(CorrelationStreamProcessor processor, boolean acceptNullLogger)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod(
                "handleInitResponse", CorrelationInitResponse.class);
        method.setAccessible(true);
        CorrelationInitResponse response = CorrelationInitResponse.newBuilder()
                .setStatus("FAILED")
                .setStatusMessage("quota unavailable")
                .build();
        try {
            method.invoke(processor, response);
        } catch (InvocationTargetException e) {
            if (!(acceptNullLogger && e.getCause() instanceof NullPointerException)) {
                throw e;
            }
        }
    }

    private void invokeHandleCorrelationResponse(CorrelationStreamProcessor processor) throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod(
                "handleCorrelationResponse", CorrelationResponse.class);
        method.setAccessible(true);
        method.invoke(processor, CorrelationResponse.newBuilder().setRequestId("missing-request").build());
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingProgressWriter implements IProgressWriter {
        private final List<String> warnings = new ArrayList<>();

        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) {}
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) { warnings.add(message); }
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }
}
