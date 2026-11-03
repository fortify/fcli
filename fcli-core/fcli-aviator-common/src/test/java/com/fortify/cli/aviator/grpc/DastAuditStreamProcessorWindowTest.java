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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fortify.aviator.dastaudit.DastAuditClientMessage;
import com.fortify.aviator.dastaudit.DastAuditDecision;
import com.fortify.aviator.dastaudit.DastAuditErrorResponse;
import com.fortify.aviator.dastaudit.DastAuditInitResponse;
import com.fortify.aviator.dastaudit.DastAuditResponse;
import com.fortify.aviator.dastaudit.DastAuditServerMessage;
import com.fortify.aviator.dastaudit.DastAuditServiceGrpc;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.dast.DastIssue;
import com.fortify.cli.common.fpr.dast.DastSession;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;

/**
 * Runs the real client against a fake server that reproduces the production behavior of
 * granting 10 new credits for every completed request.
 */
class DastAuditStreamProcessorWindowTest {
    private static final int CREDITS_PER_COMPLETION = 10;

    private final ScheduledExecutorService serverWork = Executors.newScheduledThreadPool(4);
    private final AtomicInteger serverInFlight = new AtomicInteger();
    private final AtomicInteger maxServerInFlight = new AtomicInteger();
    private final Set<String> busyOnce = ConcurrentHashMap.newKeySet();
    private final AtomicInteger busyResponses = new AtomicInteger();
    private volatile boolean rejectFirstAttemptAsBusy;
    private Server server;
    private ManagedChannel channel;

    @BeforeEach
    void startServer() throws Exception {
        server = NettyServerBuilder.forPort(0).addService(new FakeDastAuditService()).build().start();
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
    void keepsInFlightWithinWindowEvenWhenServerOverGrantsCredits() throws Exception {
        var result = process(300);

        assertEquals(300, result.results().size());
        assertTrue(result.results().stream().allMatch(r -> r instanceof DastAuditResult.Success));
        assertTrue(maxServerInFlight.get() <= 10,
            "in-flight on server exceeded window: " + maxServerInFlight.get());
    }

    @Test
    void retriesServerBusyResponsesInsteadOfFailingTheFinding() throws Exception {
        rejectFirstAttemptAsBusy = true;

        var result = process(5);

        assertEquals(5, result.results().size());
        assertTrue(result.results().stream().allMatch(r -> r instanceof DastAuditResult.Success),
            "findings rejected as busy must be retried to success");
        assertEquals(5, busyResponses.get());
    }

    private DastAuditStreamResult process(int findings) throws Exception {
        var processor = new DastAuditStreamProcessor(
            new NoopLogger(), DastAuditServiceGrpc.newStub(channel), null, 0);
        List<DastAuditWorkItem> items = new ArrayList<>();
        for (int i = 0; i < findings; i++) {
            items.add(workItem("DAST-" + i));
        }
        var config = DastAuditStreamConfig.builder().token("t").applicationName("app").build();
        return processor.process(config, items, findings).get(60, TimeUnit.SECONDS);
    }

    private static DastAuditWorkItem workItem(String issueId) {
        var issue = new DastIssue();
        issue.setId(issueId);
        return new DastAuditWorkItem(new DastSession(), issue);
    }

    private static class NoopLogger implements IAviatorLogger {
        @Override public void progress(String format, Object... args) { }
        @Override public void info(String format, Object... args) { }
        @Override public void warn(String format, Object... args) { }
        @Override public void error(String format, Object... args) { }
    }

    private class FakeDastAuditService extends DastAuditServiceGrpc.DastAuditServiceImplBase {
        @Override
        public StreamObserver<DastAuditClientMessage> processDastAuditStream(
                StreamObserver<DastAuditServerMessage> responseObserver) {
            var call = (ServerCallStreamObserver<DastAuditServerMessage>) responseObserver;
            call.disableAutoRequest();
            call.request(CREDITS_PER_COMPLETION);

            return new StreamObserver<>() {
                @Override
                public void onNext(DastAuditClientMessage message) {
                    switch (message.getRequestTypeCase()) {
                        case INIT -> send(DastAuditServerMessage.newBuilder().setInit(
                            DastAuditInitResponse.newBuilder().setStatus("SUCCESS").setUnlimitedQuota(true)).build());
                        case AUDIT -> handleAudit(message.getAudit().getRequestId());
                        default -> { }
                    }
                }

                private void handleAudit(String requestId) {
                    int now = serverInFlight.incrementAndGet();
                    maxServerInFlight.accumulateAndGet(now, Math::max);
                    serverWork.schedule(() -> {
                        serverInFlight.decrementAndGet();
                        if (rejectFirstAttemptAsBusy && busyOnce.add(requestId)) {
                            busyResponses.incrementAndGet();
                            send(DastAuditServerMessage.newBuilder().setError(DastAuditErrorResponse.newBuilder()
                                .setRequestId(requestId).setStatus("SERVER_BUSY")
                                .setStatusMessage("overloaded")).build());
                        } else {
                            send(DastAuditServerMessage.newBuilder().setAudit(DastAuditResponse.newBuilder()
                                .setRequestId(requestId).setStatus("SUCCESS")
                                .setDecision(DastAuditDecision.newBuilder().setConfidence("HIGH"))).build());
                        }
                        synchronized (call) {
                            call.request(CREDITS_PER_COMPLETION);
                        }
                    }, 20, TimeUnit.MILLISECONDS);
                }

                private void send(DastAuditServerMessage message) {
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
