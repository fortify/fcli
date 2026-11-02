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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.aviator.dastaudit.DastAuditInitResponse;
import com.fortify.aviator.dastaudit.DastAuditServerMessage;
import com.fortify.aviator.dastaudit.DastAuditServiceGrpc;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.CorrelationInitResponse;
import com.fortify.aviator.grpc.CorrelationServerMessage;
import com.fortify.aviator.grpc.CorrelationServiceGrpc;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.cli.util.FcliExecutionContextHolder;
import com.fortify.cli.common.fpr.Vulnerability;
import com.fortify.cli.common.fpr.dast.DastIssue;
import com.fortify.cli.common.fpr.dast.DastSession;
import com.fortify.cli.common.fpr.utils.SourceDecoders;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

class StreamInitializationTimeoutTest {
    private static final IAviatorLogger LOGGER = new IAviatorLogger() {
        public void progress(String format, Object... args) {}
        public void info(String format, Object... args) {}
        public void warn(String format, Object... args) {}
        public void error(String format, Object... args) {}
    };

    @Test
    void initializationWinsAgainstAnAlreadyQueuedTimeout() {
        try (var scheduler = new ManualScheduler()) {
            var expired = new AtomicBoolean();
            var timeout = new StreamInitializationTimeout(scheduler, () -> expired.set(true));
            timeout.arm();
            assertTrue(timeout.initialized());
            scheduler.fire();
            assertFalse(expired.get());
        }
    }

    @Test
    void expiredInitializationRejectsLateResponses() {
        try (var scheduler = new ManualScheduler()) {
            var expired = new AtomicBoolean();
            var timeout = new StreamInitializationTimeout(scheduler, () -> expired.set(true));
            timeout.arm();
            scheduler.fire();
            assertTrue(expired.get());
            assertTrue(timeout.isExpired());
            assertFalse(timeout.initialized());
        }
    }

    @Test
    void closingCancelsInitializationTimeout() {
        try (var scheduler = new ManualScheduler()) {
            var expired = new AtomicBoolean();
            var timeout = new StreamInitializationTimeout(scheduler, () -> expired.set(true));
            timeout.arm();
            timeout.close();
            scheduler.fire();
            assertFalse(expired.get());
            assertFalse(timeout.initialized());
        }
    }

    @Test
    void timeoutIsNotScheduledUntilArmed() {
        try (var scheduler = new ManualScheduler()) {
            var timeout = new StreamInitializationTimeout(scheduler, () -> {});
            assertFalse(scheduler.scheduled());
            timeout.arm();
            assertTrue(scheduler.scheduled());
        }
    }

    @Test
    void responseBeforeArmingDisablesTheTimeout() {
        try (var scheduler = new ManualScheduler()) {
            var timeout = new StreamInitializationTimeout(scheduler, () -> {});
            assertTrue(timeout.initialized());
            timeout.arm();
            assertFalse(scheduler.scheduled());
        }
    }

    @Test
    void missingSchedulerDisablesTheTimeoutWithoutFailing() {
        var timeout = new StreamInitializationTimeout(null, () -> {});
        timeout.arm();
        assertFalse(timeout.isExpired());
        assertTrue(timeout.initialized());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void dastTimeoutOnlyAppliesBeforeInitialization(boolean initialize) {
        try (var scheduler = new ManualScheduler()) {
            var channel = new SilentChannel();
            try (var processor = new DastAuditStreamProcessor(LOGGER,
                    DastAuditServiceGrpc.newStub(channel), scheduler, 0)) {
                var issue = new DastIssue();
                issue.setId("DAST-1");
                var result = processor.process(DastAuditStreamConfig.builder()
                    .token("token").applicationName("app").build(),
                    List.of(new DastAuditWorkItem(new DastSession(), issue)), 1);
                if (initialize) channel.respond(DastAuditServerMessage.newBuilder().setInit(
                    DastAuditInitResponse.newBuilder().setStatus("SUCCESS")).build());
                scheduler.fire();
                if (initialize) {
                    assertFalse(result.isDone());
                    assertFalse(channel.cancelled);
                } else {
                    assertThrows(CompletionException.class, result::join);
                    assertTrue(channel.cancelled);
                    int sent = channel.messages;
                    channel.respond(DastAuditServerMessage.newBuilder().setInit(
                        DastAuditInitResponse.newBuilder().setStatus("SUCCESS")).build());
                    assertEquals(sent, channel.messages, "Late initialization must not submit findings");
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void correlationTimeoutOnlyAppliesBeforeInitialization(boolean initialize) {
        try (var scheduler = new ManualScheduler()) {
            var channel = new SilentChannel();
            try (var processor = new CorrelationStreamProcessor(null, LOGGER,
                    CorrelationServiceGrpc.newStub(channel), scheduler, 0, 30)) {
                var issue = new DastIssue();
                issue.setId("DAST-1");
                issue.setSessionUrl("https://example.com");
                var bucket = new CorrelationStreamProcessor.CorrelationBucketData("SQL Injection",
                    List.of(Vulnerability.builder().instanceID("SAST-1").build()), List.of(issue));
                var result = processor.processCorrelation(new CorrelationStreamConfig("token", "app", null, null, null), List.of(bucket), "scan", Set.of());
                if (initialize) channel.respond(CorrelationServerMessage.newBuilder().setInit(
                    CorrelationInitResponse.newBuilder().setStatus("SUCCESS")).build());
                scheduler.fire();
                if (initialize) {
                    assertFalse(result.isDone());
                    assertFalse(channel.cancelled);
                } else {
                    assertThrows(CompletionException.class, result::join);
                    assertTrue(channel.cancelled);
                    int sent = channel.messages;
                    channel.respond(CorrelationServerMessage.newBuilder().setInit(
                        CorrelationInitResponse.newBuilder().setStatus("SUCCESS")).build());
                    assertEquals(sent, channel.messages, "Late initialization must not submit findings");
                }
            }
        }
    }

    @Test
    void sastInitializationTimeoutReleasesCallingThreadAndCancelsRpc() throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        var caller = Executors.newSingleThreadExecutor();
           try (var context = FcliExecutionContextHolder.pushNew();
               var scheduler = new ManualScheduler();
             var client = AviatorGrpcClientHelper.createClient("127.0.0.1:1", AviatorRequestPolicy.WAIT_FOR_READY)) {
            var channel = new SilentChannel();
            try (var processor = new AviatorStreamProcessor(client, LOGGER,
                    AuditorServiceGrpc.newStub(channel), workers, scheduler, 30, 30,
                    null, SourceDecoders.defaults(), null)) {
                var requests = new ArrayDeque<UserPrompt>();
                requests.add(UserPrompt.builder().issueData(IssueData.builder().instanceID("SAST-1").build()).build());
                var invocation = caller.submit(() -> processor.processBatchRequests(
                    requests, "app", "build", "ssc-app", "version", "token", List.of()));
                assertTrue(channel.sent.await(5, TimeUnit.SECONDS));
                assertTrue(scheduler.armed.await(5, TimeUnit.SECONDS));
                assertFalse(invocation.isDone(), "The caller waits for initialization");
                scheduler.fire();
                var result = invocation.get(5, TimeUnit.SECONDS);
                assertThrows(CompletionException.class, result::join);
                assertTrue(channel.cancelled);
            }
        } finally {
            caller.shutdownNow();
            workers.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void sastInitializationTimeoutReleasesCallerOnRealChannel(boolean silentServer) throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        var caller = Executors.newSingleThreadExecutor();
           try (var context = FcliExecutionContextHolder.pushNew();
               var scheduler = new ManualScheduler();
             var silent = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             var client = AviatorGrpcClientHelper.createClient("127.0.0.1:1", AviatorRequestPolicy.WAIT_FOR_READY)) {
            // Unreachable port: wait-for-ready keeps the call queued. Silent socket: connects, never answers.
            int port = silentServer ? silent.getLocalPort() : 1;
            var channel = NettyChannelBuilder.forAddress("127.0.0.1", port).usePlaintext().build();
            try (var processor = new AviatorStreamProcessor(client, LOGGER,
                    AuditorServiceGrpc.newStub(channel).withWaitForReady(), workers, scheduler, 30, 30,
                    null, SourceDecoders.defaults(), null)) {
                var requests = new ArrayDeque<UserPrompt>();
                requests.add(UserPrompt.builder().issueData(IssueData.builder().instanceID("SAST-1").build()).build());
                var invocation = caller.submit(() -> processor.processBatchRequests(
                    requests, "app", "build", "ssc-app", "version", "token", List.of()));
                assertTrue(scheduler.armed.await(5, TimeUnit.SECONDS));
                assertFalse(invocation.isDone(), "The caller waits for initialization");
                scheduler.fire();
                var result = invocation.get(10, TimeUnit.SECONDS);
                var failure = assertThrows(CompletionException.class, result::join);
                assertTrue(failure.getCause().getMessage().contains("initialization timed out"));
            } finally {
                channel.shutdownNow();
            }
        } finally {
            caller.shutdownNow();
            workers.shutdownNow();
        }
    }

    private static final class ManualScheduler extends ScheduledThreadPoolExecutor implements AutoCloseable {
        private volatile Runnable task;
        private final CountDownLatch armed = new CountDownLatch(1);
        ManualScheduler() { super(1); }
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            assertEquals(30, unit.toSeconds(delay));
            task = command;
            armed.countDown();
            return super.schedule(command, 1, TimeUnit.DAYS);
        }
        void fire() { task.run(); }
        boolean scheduled() { return task != null; }
        @Override
        public void close() { shutdownNow(); }
    }

    private static final class SilentChannel extends Channel {
        private ClientCall.Listener<Object> listener;
        private volatile boolean cancelled;
        private final CountDownLatch sent = new CountDownLatch(1);
        private int messages;
        @Override
        public String authority() { return "test"; }
        @Override
        @SuppressWarnings("unchecked")
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions options) {
            return new ClientCall<>() {
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    listener = (Listener<Object>) responseListener;
                }
                public void request(int count) {}
                public void cancel(String message, Throwable cause) {
                    cancelled = true;
                    listener.onClose(Status.CANCELLED.withCause(cause), new Metadata());
                }
                public void halfClose() {}
                public void sendMessage(ReqT message) { messages++; sent.countDown(); }
            };
        }
        void respond(Object response) { listener.onMessage(response); }
    }
}
