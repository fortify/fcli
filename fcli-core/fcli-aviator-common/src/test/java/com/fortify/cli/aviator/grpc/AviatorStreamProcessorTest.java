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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.utils.SourceDecoders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.grpc.stub.ClientResponseObserver;

class AviatorStreamProcessorTest {
    private static final String REQUEST_ID = "request-1";
    private static final String ISSUE_ID = "SAST-1";

    private final RecordingLogger logger = new RecordingLogger();
    private final Logger log = (Logger) LoggerFactory.getLogger(AviatorStreamProcessor.class);
    private final ListAppender<ILoggingEvent> events = new ListAppender<>();
    private Level previousLevel;
    private boolean previousAdditive;

    @BeforeEach
    void captureLogs() {
        previousLevel = log.getLevel();
        previousAdditive = log.isAdditive();
        log.setLevel(Level.DEBUG);
        log.setAdditive(false);
        events.start();
        log.addAppender(events);
    }

    @AfterEach
    void restoreLogs() {
        log.detachAppender(events);
        events.stop();
        log.setLevel(previousLevel);
        log.setAdditive(previousAdditive);
    }

    @Test
    void exhaustedServerBusyRetryLogsOneErrorAndCompletesResult() throws Exception {
        var processor = new AviatorStreamProcessor(null, logger, null, null, null, 0, 0,
                null, SourceDecoders.defaults(), null);
        var responses = new LinkedHashMap<String, AuditResponse>();
        var processedRequests = new AtomicInteger();
        var resultFuture = new CompletableFuture<Map<String, AuditResponse>>();
        var streamLatch = new CountDownLatch(1);
        var initLatch = new CountDownLatch(1);
        var wrapper = new RequestWrapper(UserPrompt.builder()
                .issueData(IssueData.builder().instanceID(ISSUE_ID).build())
                .build());
        wrapper.attemptCount = Constants.MAX_RETRIES;
        wrapper.outstandingTracked = true;

        var state = new StreamState("stream-1", "app", "build", "ssc-app", "version", "token", 1);
        state.pendingIssueIds.add(ISSUE_ID);
        setField(processor, "currentStreamState", state);
        Map<String, RequestWrapper> inflightRequests = getMapField(processor, "inflightRequests");
        inflightRequests.put(REQUEST_ID, wrapper);
        Map<String, RequestMetrics> requestMetrics = getMapField(processor, "requestMetricsMap");
        requestMetrics.put(REQUEST_ID, new RequestMetrics());
        getField(processor, "outstandingRequests", AtomicInteger.class).set(1);
        var requestSemaphore = getField(processor, "requestSemaphore", Semaphore.class);
        assertTrue(requestSemaphore.tryAcquire());

        var observer = createResponseObserver(processor, responses, processedRequests,
                resultFuture, streamLatch, initLatch);

        observer.onNext(serverBusyResponse());

        assertEquals(1, processedRequests.get());
        assertEquals(0, streamLatch.getCount());
        assertTrue(resultFuture.isDone());
        assertEquals(0, getField(processor, "outstandingRequests", AtomicInteger.class).get());
        assertEquals(Constants.INITIAL_REQUEST_WINDOW, requestSemaphore.availablePermits());
        assertEquals(1, responses.size());
        var failedResponse = responses.get(ISSUE_ID);
        assertNotNull(failedResponse);
        assertEquals("RETRY_LIMIT_EXCEEDED", failedResponse.getStatus());
        assertTrue(failedResponse.getStatusMessage().contains("retries"));
        assertTrue(failedResponse.getStatusMessage().contains("server overload"));
        assertTrue(failedResponse.isSubmittedToAviator());
        assertEquals(responses, resultFuture.join());
        assertEquals(1, logger.progressMessages.size());

        observer.onNext(serverBusyResponse());

        assertEquals(1, processedRequests.get());
        assertEquals(1, responses.size());
        assertEquals(1, logger.progressMessages.size());
        assertEquals(Constants.INITIAL_REQUEST_WINDOW, requestSemaphore.availablePermits());
        assertEquals(0, getField(processor, "outstandingRequests", AtomicInteger.class).get());
        assertEquals(1, errorEvents().size());
        var errorMessage = errorEvents().get(0).getFormattedMessage();
        assertTrue(errorMessage.contains(ISSUE_ID));
        assertTrue(errorMessage.contains(Constants.MAX_RETRIES + " retries"));
        assertTrue(events.list.stream().noneMatch(event -> event.getLevel() == Level.WARN));
        assertTrue(logger.warnings.isEmpty());
    }

    private static AuditorResponse serverBusyResponse() {
        return AuditorResponse.newBuilder()
                .setRequestId(REQUEST_ID)
                .setStatus("SERVER_BUSY")
                .build();
    }

    @SuppressWarnings("unchecked")
    private static ClientResponseObserver<UserPromptRequest, AuditorResponse> createResponseObserver(
            AviatorStreamProcessor processor, Map<String, AuditResponse> responses,
            AtomicInteger processedRequests, CompletableFuture<Map<String, AuditResponse>> resultFuture,
            CountDownLatch streamLatch, CountDownLatch initLatch) throws Exception {
        var method = AviatorStreamProcessor.class.getDeclaredMethod("createResponseObserver",
                Map.class, AtomicInteger.class, CompletableFuture.class, CountDownLatch.class,
                CountDownLatch.class, StreamInitializationTimeout.class);
        method.setAccessible(true);
        return (ClientResponseObserver<UserPromptRequest, AuditorResponse>) method.invoke(processor,
                responses, processedRequests, resultFuture, streamLatch, initLatch,
                new StreamInitializationTimeout(null, () -> {}));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(AviatorStreamProcessor processor, String name, Class<T> type)
            throws Exception {
        var field = AviatorStreamProcessor.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(processor);
    }

    private static void setField(AviatorStreamProcessor processor, String name, Object value)
            throws Exception {
        var field = AviatorStreamProcessor.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(processor, value);
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getMapField(AviatorStreamProcessor processor, String name)
            throws Exception {
        return (Map<K, V>) getField(processor, name, Map.class);
    }

    private List<ILoggingEvent> errorEvents() {
        return events.list.stream().filter(event -> event.getLevel() == Level.ERROR).toList();
    }

    private static final class RecordingLogger implements IAviatorLogger {
        private final List<String> progressMessages = new ArrayList<>();
        private final List<String> warnings = new ArrayList<>();

        @Override public void progress(String format, Object... args) {
            progressMessages.add(String.format(format, args));
        }
        @Override public void info(String format, Object... args) {}
        @Override public void warn(String format, Object... args) {
            warnings.add(String.format(format, args));
        }
        @Override public void error(String format, Object... args) {}
    }
}
