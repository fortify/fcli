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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.dastaudit.DastAuditClientMessage;
import com.fortify.aviator.dastaudit.DastAuditErrorResponse;
import com.fortify.aviator.dastaudit.DastAuditResponse;
import com.fortify.aviator.dastaudit.DastAuditServerMessage;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.dast.DastIssue;
import com.fortify.cli.common.fpr.dast.DastSession;
import com.fortify.cli.common.progress.helper.IProgressWriter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;

class DastAuditStreamProcessorTest {
    private final RecordingProgressWriter progress = new RecordingProgressWriter();
    private final IAviatorLogger logger = new AviatorLoggerImpl(progress);
    private final Logger log = (Logger) LoggerFactory.getLogger(DastAuditStreamProcessor.class);
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
    void registersAllRequestsBeforeReturningMessagesForSending() {
        var processor = new DastAuditStreamProcessor(null, null, null, 0);

        List<DastAuditClientMessage> requests = processor.prepareAuditRequests(
            List.of(workItem("DAST-1"), workItem("DAST-2")), "stream-1");

        assertEquals(List.of("DAST-1", "DAST-2"), requests.stream()
            .map(request -> request.getAudit().getFinding().getIssueId())
            .toList());
        assertEquals(2, processor.pendingRequestCount());
    }

    @Test
    void reusesRequestIdsAndRequeuesOnlyUnfinishedWork() {
        var processor = new DastAuditStreamProcessor(null, null, null, 0);
        List<DastAuditWorkItem> workItems = List.of(workItem("DAST-1"), workItem("DAST-2"));
        processor.initializeWorkItems(workItems);
        List<DastAuditClientMessage> firstAttempt = processor.prepareAuditRequests(workItems, "stream-1");

        processor.completeRequest(firstAttempt.get(0).getAudit().getRequestId());
        List<DastAuditClientMessage> retry = processor.prepareAuditRequests(
            processor.remainingWorkItems(), "stream-2");

        assertEquals(1, retry.size());
        assertEquals("DAST-2", retry.get(0).getAudit().getFinding().getIssueId());
        assertEquals(firstAttempt.get(1).getAudit().getRequestId(), retry.get(0).getAudit().getRequestId());
        assertEquals("stream-2", retry.get(0).getAudit().getStreamId());
    }

    @Test
    void retriesOnlyTransportDisconnections() {
        assertTrue(DastAuditStreamProcessor.isRetryableError(Status.UNAVAILABLE.asRuntimeException()));
        assertTrue(DastAuditStreamProcessor.isRetryableError(
            Status.INTERNAL.withDescription("RST_STREAM closed").asRuntimeException()));
        assertTrue(DastAuditStreamProcessor.isInfiniteRetryError(
            Status.INTERNAL.withDescription("PROTOCOL_ERROR").asRuntimeException()));
        assertFalse(DastAuditStreamProcessor.isRetryableError(Status.INVALID_ARGUMENT.asRuntimeException()));
    }

    @Test
    void unknownResponseTypeWarnsWithoutChangingPendingWorkOrCapacity() throws Exception {
        var processor = new DastAuditStreamProcessor(logger, null, null, 0);
        var requests = fillWindow(processor);
        var result = new CompletableFuture<DastAuditStreamResult>();
        var observer = responseObserver(processor, result);

        observer.onNext(DastAuditServerMessage.getDefaultInstance());

        assertEquals(List.of("WARN: Unknown DAST audit response type: RESPONSETYPE_NOT_SET"), progress.warnings);
        assertUnchangedWork(processor, requests.size(), 0, result);
    }

    @Test
    void unrecognizedAuditResponseWarnsWithoutChangingPendingWorkOrCapacity() throws Exception {
        var processor = new DastAuditStreamProcessor(logger, null, null, 0);
        var requests = fillWindow(processor);
        var result = new CompletableFuture<DastAuditStreamResult>();
        var observer = responseObserver(processor, result);

        observer.onNext(auditResponse("unrecognized-request"));

        assertEquals(List.of("WARN: Ignoring DAST audit response for unrecognized request unrecognized-request"),
            progress.warnings);
        assertUnchangedWork(processor, requests.size(), 0, result);
        assertEquals("DAST-0", processor.completeRequest(requests.get(0).getAudit().getRequestId()));
        assertEquals(1, window(processor).drainSendable().size(), "Only a recognized completion frees capacity");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void completedDuplicateStaysAtDebugWithoutAdvancingWork(boolean error) throws Exception {
        var processor = new DastAuditStreamProcessor(logger, null, null, 0);
        var requests = fillWindow(processor);
        var result = new CompletableFuture<DastAuditStreamResult>();
        var observer = responseObserver(processor, result);
        var requestId = requests.get(0).getAudit().getRequestId();
        observer.onNext(auditResponse(requestId));
        window(processor).drainSendable();
        progress.messages.clear();
        events.list.clear();

        observer.onNext(error
            ? DastAuditServerMessage.newBuilder().setError(DastAuditErrorResponse.newBuilder()
                .setRequestId(requestId).setStatus("FAILED")).build()
            : auditResponse(requestId));

        assertTrue(progress.warnings.isEmpty());
        assertEquals(1, events.list.size());
        assertEquals(Level.DEBUG, events.list.get(0).getLevel());
        assertTrue(events.list.get(0).getFormattedMessage().contains(requestId));
        assertUnchangedWork(processor, requests.size() - 1, 1, result);
    }

    @Test
    void unrecognizedErrorStillFailsTheStream() throws Exception {
        var processor = new DastAuditStreamProcessor(logger, null, null, 0);
        var result = new CompletableFuture<DastAuditStreamResult>();
        var observer = responseObserver(processor, result);

        observer.onNext(DastAuditServerMessage.newBuilder().setError(DastAuditErrorResponse.newBuilder()
            .setRequestId("unrecognized-request").setStatus("FAILED").setStatusMessage("request failed")).build());

        assertTrue(result.isCompletedExceptionally());
    }

    private List<DastAuditClientMessage> fillWindow(DastAuditStreamProcessor processor) throws Exception {
        var items = new ArrayList<DastAuditWorkItem>();
        for (int i = 0; i < Constants.INITIAL_REQUEST_WINDOW + 2; i++) {
            items.add(workItem("DAST-" + i));
        }
        processor.initializeWorkItems(items);
        var requests = processor.prepareAuditRequests(items, "stream");
        window(processor).enqueue(requests);
        assertEquals(Constants.INITIAL_REQUEST_WINDOW, window(processor).drainSendable().size());
        return requests;
    }

    private void assertUnchangedWork(DastAuditStreamProcessor processor, int pending, int completed,
            CompletableFuture<DastAuditStreamResult> result) throws Exception {
        assertEquals(pending, processor.pendingRequestCount());
        assertEquals(pending, processor.remainingWorkItems().size());
        assertEquals(Constants.INITIAL_REQUEST_WINDOW, window(processor).inFlightCount());
        assertTrue(window(processor).drainSendable().isEmpty(), "No extra capacity may be released");
        var field = DastAuditStreamProcessor.class.getDeclaredField("results");
        field.setAccessible(true);
        assertEquals(completed, assertInstanceOf(List.class, field.get(processor)).size());
        assertTrue(progress.messages.isEmpty(), "Ignored responses must not advance progress");
        assertFalse(result.isDone());
    }

    private DastAuditServerMessage auditResponse(String requestId) {
        return DastAuditServerMessage.newBuilder().setAudit(DastAuditResponse.newBuilder()
            .setRequestId(requestId).setStatus("SUCCESS")).build();
    }

    @SuppressWarnings("unchecked")
    private StreamObserver<DastAuditServerMessage> responseObserver(DastAuditStreamProcessor processor,
            CompletableFuture<DastAuditStreamResult> result) throws Exception {
        var field = DastAuditStreamProcessor.class.getDeclaredField("resultFuture");
        field.setAccessible(true);
        field.set(processor, result);
        var type = Class.forName(DastAuditStreamProcessor.class.getName() + "$ResponseObserver");
        var constructor = type.getDeclaredConstructor(
            DastAuditStreamProcessor.class, long.class, StreamInitializationTimeout.class);
        constructor.setAccessible(true);
        return (StreamObserver<DastAuditServerMessage>) constructor.newInstance(
            processor, 0L, new StreamInitializationTimeout(null, () -> {}));
    }

    @SuppressWarnings("unchecked")
    private RequestSendWindow<DastAuditClientMessage> window(DastAuditStreamProcessor processor) throws Exception {
        var field = DastAuditStreamProcessor.class.getDeclaredField("sendWindow");
        field.setAccessible(true);
        return (RequestSendWindow<DastAuditClientMessage>) field.get(processor);
    }

    private DastAuditWorkItem workItem(String issueId) {
        var issue = new DastIssue();
        issue.setId(issueId);
        return new DastAuditWorkItem(new DastSession(), issue);
    }

    private static final class RecordingProgressWriter implements IProgressWriter {
        private final List<String> warnings = new ArrayList<>();
        private final List<String> messages = new ArrayList<>();

        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) { messages.add(message); }
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) { warnings.add(message); }
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }
}