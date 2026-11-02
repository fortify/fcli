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
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.aviator.grpc.CorrelationInitResponse;
import com.fortify.aviator.grpc.CorrelationResponse;
import com.fortify.aviator.grpc.CorrelationServerMessage;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.dast.DastIssue;
import com.fortify.cli.common.progress.helper.IProgressWriter;

class CorrelationStreamProcessorTest {
    private final RecordingProgressWriter progress = new RecordingProgressWriter();
    private final IAviatorLogger logger = new AviatorLoggerImpl(progress);

    @Test
    void buildCorrelationWorkItemsPreservesDastSessionUrlEncounterOrder() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);
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
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);
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
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);
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
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);

        invokeOnNext(processor, CorrelationServerMessage.newBuilder().build());

        assertEquals(List.of("WARN: Unknown correlation response type: RESPONSETYPE_NOT_SET"), progress.warnings);
    }

    @Test
    void nonOkInitResponseIsWarnedOnce() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);
        prepareInitializedStream(processor);

        invokeHandleInitResponse(processor, "FAILED");

        assertEquals(List.of("WARN: Init response status: FAILED — quota unavailable"), progress.warnings);
        CorrelationStreamState state = (CorrelationStreamState) getField(processor, "state");
        assertTrue(state.isStreamInitialized);
        assertEquals(CorrelationStreamState.Phase.CORRELATING, state.currentPhase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"OK", "SUCCESS", "ok", "success"})
    void successfulInitResponseDoesNotWarn(String status) throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);
        prepareInitializedStream(processor);

        invokeHandleInitResponse(processor, status);

        assertTrue(progress.warnings.isEmpty());
    }

    @Test
    void unknownOrCompletedCorrelationRequestIsWarnedOnce() throws Exception {
        CorrelationStreamProcessor processor = new CorrelationStreamProcessor(null, logger, null, null, 0, 0);

        invokeHandleCorrelationResponse(processor);

        assertEquals(List.of("WARN: Ignoring correlation response for unknown or completed request missing-request"),
                progress.warnings);
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

    private Object getField(CorrelationStreamProcessor processor, String fieldName) throws Exception {
        Field field = CorrelationStreamProcessor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(processor);
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

    private void invokeHandleInitResponse(CorrelationStreamProcessor processor, String status)
            throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod(
                "handleInitResponse", CorrelationInitResponse.class);
        method.setAccessible(true);
        CorrelationInitResponse response = CorrelationInitResponse.newBuilder()
                .setStatus(status)
                .setStatusMessage("quota unavailable")
                .build();
        method.invoke(processor, response);
    }

    private void invokeHandleCorrelationResponse(CorrelationStreamProcessor processor) throws Exception {
        Method method = CorrelationStreamProcessor.class.getDeclaredMethod(
                "handleCorrelationResponse", CorrelationResponse.class);
        method.setAccessible(true);
        method.invoke(processor, CorrelationResponse.newBuilder().setRequestId("missing-request").build());
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
