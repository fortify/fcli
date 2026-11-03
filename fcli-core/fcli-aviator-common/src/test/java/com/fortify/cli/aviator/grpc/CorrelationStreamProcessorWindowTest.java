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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortify.aviator.grpc.CorrelationCandidateMatch;
import com.fortify.aviator.grpc.CorrelationClientMessage;
import com.fortify.aviator.grpc.CorrelationDecision;
import com.fortify.aviator.grpc.CorrelationErrorResponse;
import com.fortify.aviator.grpc.CorrelationInitResponse;
import com.fortify.aviator.grpc.CorrelationResponse;
import com.fortify.aviator.grpc.CorrelationServerMessage;
import com.fortify.aviator.grpc.CorrelationServiceGrpc;
import com.fortify.aviator.grpc.CorrelationValidationResponse;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.dast.DastIssue;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * Runs the real correlation client against a fake server that reproduces the production behavior
 * of granting 10 new credits for every completed request.
 */
class CorrelationStreamProcessorWindowTest {
    private static final int CREDITS_PER_COMPLETION = 10;
    private static final String DAST_URL = "https://example.com/login";

    private final ScheduledExecutorService serverWork = Executors.newScheduledThreadPool(4);
    private final AtomicInteger serverInFlight = new AtomicInteger();
    private final AtomicInteger maxServerInFlight = new AtomicInteger();
    private final Set<String> busyOnce = ConcurrentHashMap.newKeySet();
    private final AtomicInteger busyResponses = new AtomicInteger();
    private volatile boolean rejectFirstAttemptAsBusy;
    private volatile String failCorrelationForSast;
    private volatile String failValidationForSast;
    private volatile boolean failStreamOnFirstRequest;
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void startServer() throws Exception {
        server = NettyServerBuilder.forPort(0).addService(new FakeCorrelationService()).build().start();
        channel = NettyChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
    }

    @AfterEach
    void stopServer() throws Exception {
        channel.shutdownNow();
        server.shutdownNow();
        serverWork.shutdownNow();
        server.awaitTermination(5, TimeUnit.SECONDS);
    }

    @Test
    void keepsInFlightWithinWindowInBothPhasesEvenWhenServerOverGrantsCredits() throws Exception {
        var result = process(200);

        assertEquals(200, result.successfulCorrelationResponses());
        assertEquals(200, result.confirmedPairs().size());
        assertTrue(maxServerInFlight.get() <= 10,
            "in-flight on server exceeded window: " + maxServerInFlight.get());
    }

    @Test
    void retriesServerBusyResponsesInsteadOfFailingTheRun() throws Exception {
        rejectFirstAttemptAsBusy = true;

        var result = process(5);

        assertEquals(5, result.successfulCorrelationResponses());
        assertEquals(5, result.confirmedPairs().size());
        // every correlation and every validation request is rejected once
        assertEquals(10, busyResponses.get());
    }

    @Test
    void perRequestErrorsFailOnlyThatItemAndTheRunCompletes() throws Exception {
        failCorrelationForSast = "SAST-0";
        failValidationForSast = "SAST-1";

        var result = process(5);

        assertEquals(4, result.successfulCorrelationResponses());
        assertEquals(1, result.failedCorrelationResponses());
        assertEquals(1, result.failedValidationResponses());
        assertEquals(3, result.confirmedPairs().size());
        assertTrue(result.rejectedPairs().isEmpty(), "failed validation must not be persisted as rejected");
    }

    @Test
    void streamLevelErrorWithoutRequestIdFailsTheRun() {
        failStreamOnFirstRequest = true;

        var ex = assertThrows(ExecutionException.class, () -> process(5));
        assertTrue(ex.getCause().getMessage().contains("Internal server error"));
    }

    private CorrelationResult process(int sastFindings) throws Exception {
        var processor = new CorrelationStreamProcessor(
            null, new NoopLogger(), CorrelationServiceGrpc.newStub(channel), null, 0, 0);
        List<Vulnerability> vulns = new ArrayList<>();
        for (int i = 0; i < sastFindings; i++) {
            vulns.add(Vulnerability.builder().instanceID("SAST-" + i).build());
        }
        var dast = new DastIssue();
        dast.setId("DAST-1");
        dast.setSessionUrl(DAST_URL);
        var bucket = new CorrelationStreamProcessor.CorrelationBucketData("SQL Injection", vulns, List.of(dast));
        var config = new CorrelationStreamConfig("t", "app", null, null, null);
        return processor.processCorrelation(config, List.of(bucket), "scan-guid").get(60, TimeUnit.SECONDS);
    }

    private static class NoopLogger implements IAviatorLogger {
        @Override public void progress(String format, Object... args) { }
        @Override public void info(String format, Object... args) { }
        @Override public void warn(String format, Object... args) { }
        @Override public void error(String format, Object... args) { }
    }

    private class FakeCorrelationService extends CorrelationServiceGrpc.CorrelationServiceImplBase {
        @Override
        public StreamObserver<CorrelationClientMessage> processCorrelationStream(
                StreamObserver<CorrelationServerMessage> responseObserver) {
            var call = (ServerCallStreamObserver<CorrelationServerMessage>) responseObserver;
            call.disableAutoRequest();
            call.request(CREDITS_PER_COMPLETION);

            return new StreamObserver<>() {
                @Override
                public void onNext(CorrelationClientMessage message) {
                    switch (message.getRequestTypeCase()) {
                        case INIT -> send(CorrelationServerMessage.newBuilder().setInit(
                            CorrelationInitResponse.newBuilder().setStatus("SUCCESS").setUnlimitedQuota(true)).build());
                        case CORRELATION -> handle(message.getCorrelation().getRequestId(),
                            message.getCorrelation().getSastFinding().getInstanceId().equals(failCorrelationForSast),
                            () -> CorrelationServerMessage.newBuilder().setCorrelation(CorrelationResponse.newBuilder()
                                .setRequestId(message.getCorrelation().getRequestId())
                                .setSastId(message.getCorrelation().getSastFinding().getInstanceId())
                                .setStatus("OK")
                                .addMatches(CorrelationCandidateMatch.newBuilder().setUrl(DAST_URL))).build());
                        case VALIDATION -> handle(message.getValidation().getRequestId(),
                            message.getValidation().getSastFinding().getInstanceId().equals(failValidationForSast),
                            () -> CorrelationServerMessage.newBuilder().setValidation(CorrelationValidationResponse.newBuilder()
                                .setRequestId(message.getValidation().getRequestId())
                                .setSastId(message.getValidation().getSastFinding().getInstanceId())
                                .setDastId(message.getValidation().getDastIssue().getDastIssueId())
                                .setStatus("OK")
                                .setDecision(CorrelationDecision.newBuilder().setConfirmed(true))).build());
                        default -> { }
                    }
                }

                private void handle(String requestId, boolean fail,
                        java.util.function.Supplier<CorrelationServerMessage> success) {
                    if (failStreamOnFirstRequest) {
                        send(error("", "INTERNAL_ERROR", "Internal server error"));
                        return;
                    }
                    int now = serverInFlight.incrementAndGet();
                    maxServerInFlight.accumulateAndGet(now, Math::max);
                    serverWork.schedule(() -> {
                        serverInFlight.decrementAndGet();
                        if (rejectFirstAttemptAsBusy && busyOnce.add(requestId)) {
                            busyResponses.incrementAndGet();
                            send(error(requestId, "SERVER_BUSY", "overloaded"));
                        } else if (fail) {
                            send(error(requestId, "REQUEST_PROCESSING_ERROR", "failed"));
                        } else {
                            send(success.get());
                        }
                        synchronized (call) {
                            call.request(CREDITS_PER_COMPLETION);
                        }
                    }, 20, TimeUnit.MILLISECONDS);
                }

                private CorrelationServerMessage error(String requestId, String status, String message) {
                    return CorrelationServerMessage.newBuilder().setError(CorrelationErrorResponse.newBuilder()
                        .setRequestId(requestId).setStatus(status).setStatusMessage(message)).build();
                }

                private void send(CorrelationServerMessage message) {
                    synchronized (call) {
                        call.onNext(message);
                    }
                }

                @Override
                public void onError(Throwable t) { }

                @Override
                public void onCompleted() {
                    synchronized (call) {
                        call.onCompleted();
                    }
                }
            };
        }
    }
}
