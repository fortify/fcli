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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.fortify.aviator.grpc.AuditResult;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.SourceDecoders;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

class AviatorPartialAuditTest {
    enum Outcome {
        PARTIAL_FAILURE, PARTIAL_INTERNAL_ERROR, PARTIAL_BACKPRESSURE, TOTAL_FAILURE, EMPTY_AUDIT_FAILURE,
        INITIALIZATION_REJECTED, COMPLETED, COMPLETED_WITH_ISSUE_FAILURE, EARLY_COMPLETION, TRANSIENT_FAILURE
    }

    @TempDir
    Path tempDir;

    @Test
    @Timeout(15)
    void unexpectedImplementationFailureAfterSuccessRemainsExceptional() throws Exception {
        var implementationFailure = new IllegalStateException("Injected implementation failure");
        var firstResponse = new CountDownLatch(1);
        var submissions = new AtomicInteger();
        var logger = new NoopLogger() {
            @Override
            public void progress(String format, Object... args) {
                if (format.startsWith("Processed 1 out of")) {
                    firstResponse.countDown();
                }
            }

            @Override
            public void info(String format, Object... args) {
                if ("Size of files {}".equals(format) && submissions.incrementAndGet() == 2) {
                    try {
                        if (!firstResponse.await(5, TimeUnit.SECONDS)) {
                            throw new AssertionError("First success was not received");
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                    throw implementationFailure;
                }
            }
        };
        var server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", 0))
                .addService(new FakeAuditService(Outcome.COMPLETED)).build().start();
        var channel = NettyChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
        try (var client = new AviatorGrpcClient(channel, 5, logger, 30, AviatorRequestPolicy.WAIT_FOR_READY, null)) {
            var requests = new ConcurrentLinkedDeque<>(List.of(prompt("issue-1"), prompt("issue-2")));
            var future = client.processBatchRequests(requests, "app", "build", "ssc-app", "version", "test-token",
                    null, null, SourceDecoders.defaults(), new FVDLMetadata());
            var failure = assertThrows(ExecutionException.class, () -> future.get(5, TimeUnit.SECONDS));
            assertInstanceOf(AviatorTechnicalException.class, failure.getCause());
            assertSame(implementationFailure, failure.getCause().getCause());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    @ParameterizedTest
    @EnumSource(Outcome.class)
    @Timeout(20)
    void preservesReceivedAuditsWithoutHidingTerminalFailure(Outcome outcome) throws Exception {
        Path fpr = tempDir.resolve("input.fpr");
        try (var zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            zip.putNextEntry(new ZipEntry("audit.fvdl"));
            zip.write("<FVDL/>".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        var service = new FakeAuditService(outcome);
        var server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", 0))
                .addService(service).build().start();
        var channel = NettyChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
        try (var handle = new FprHandle(fpr);
             var client = new AviatorGrpcClient(channel, 5, new NoopLogger(), 30,
                     AviatorRequestPolicy.WAIT_FOR_READY, null)) {
            var requests = new ConcurrentLinkedDeque<>(List.of(prompt("issue-1"), prompt("issue-2")));
            var future = client.processBatchRequests(requests, "app", "build", "ssc-app", "version", "test-token",
                    handle, null, SourceDecoders.defaults(), new FVDLMetadata());
            var result = future.get(10, TimeUnit.SECONDS);
            if (outcome == Outcome.COMPLETED || outcome == Outcome.COMPLETED_WITH_ISSUE_FAILURE
                    || outcome == Outcome.EARLY_COMPLETION || outcome == Outcome.TRANSIENT_FAILURE) {
                var completed = assertInstanceOf(SastStreamOutcome.Completed.class, result);
                assertEquals(outcome == Outcome.EARLY_COMPLETION ? 1 : 2, completed.responses().size());
                if (outcome != Outcome.EARLY_COMPLETION) {
                    assertEquals(outcome == Outcome.COMPLETED_WITH_ISSUE_FAILURE ? "FAILED" : "SUCCESS",
                            completed.responses().get("issue-2").getStatus());
                }
            } else {
                var failed = assertInstanceOf(SastStreamOutcome.Failed.class, result);
                assertInstanceOf(AviatorTechnicalException.class, failed.failure());
                assertEquals(outcome == Outcome.TOTAL_FAILURE || outcome == Outcome.INITIALIZATION_REJECTED
                                ? Set.of() : Set.of("issue-1"),
                        failed.responses().keySet());
                if (outcome == Outcome.PARTIAL_INTERNAL_ERROR) {
                    assertEquals("Internal server error occurred", failed.failure().getMessage());
                } else if (outcome == Outcome.PARTIAL_BACKPRESSURE) {
                    assertEquals("Stream terminated by server: Injected backpressure violation", failed.failure().getMessage());
                } else if (outcome == Outcome.INITIALIZATION_REJECTED) {
                    assertEquals("Stream initialization failed: Injected initialization rejection", failed.failure().getMessage());
                } else {
                    var transportFailure = assertInstanceOf(StatusRuntimeException.class, failed.failure().getCause());
                    assertEquals(Status.Code.INVALID_ARGUMENT, transportFailure.getStatus().getCode());
                    assertEquals("Injected terminal failure", transportFailure.getStatus().getDescription());
                }
            }
            assertEquals(outcome == Outcome.TRANSIENT_FAILURE ? 2 : 1, service.streamAttempts.get());
            assertThrows(UnsupportedOperationException.class, () -> result.responses().clear());
        } finally {
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private UserPrompt prompt(String issueId) {
        return UserPrompt.builder().issueData(IssueData.builder().instanceID(issueId)
                .accuracy("").confidence("").impact("").likelihood("").probability("").build())
                .stackTrace(List.of()).files(new ArrayList<>()).programmingLanguages(Set.of("java")).build();
    }

    private static class NoopLogger implements IAviatorLogger {
        @Override public void progress(String format, Object... args) { }
        @Override public void info(String format, Object... args) { }
        @Override public void warn(String format, Object... args) { }
        @Override public void error(String format, Object... args) { }
    }

    private static class FakeAuditService extends AuditorServiceGrpc.AuditorServiceImplBase {
        private final Outcome outcome;
        private final AtomicInteger streamAttempts = new AtomicInteger();

        private FakeAuditService(Outcome outcome) {
            this.outcome = outcome;
        }

        @Override
        public StreamObserver<UserPromptRequest> processStream(StreamObserver<AuditorResponse> responses) {
            int attempt = streamAttempts.incrementAndGet();
            return new StreamObserver<>() {
                private int audited;
                private boolean terminated;

                @Override
                public void onNext(UserPromptRequest request) {
                    if (terminated) {
                        return;
                    }
                    if (request.hasInit()) {
                        var init = AuditorResponse.newBuilder().setStatus("SUCCESS")
                                .setRequestId(request.getInit().getRequestId());
                        if (outcome == Outcome.INITIALIZATION_REJECTED) {
                            init.setStatus("FAILED").setStatusMessage("Injected initialization rejection");
                        }
                        responses.onNext(init.build());
                    } else if (request.hasAudit()) {
                        if (outcome == Outcome.EARLY_COMPLETION && audited > 0) {
                            terminated = true;
                            responses.onCompleted();
                        } else if (outcome == Outcome.TRANSIENT_FAILURE && attempt == 1 && audited > 0) {
                            terminated = true;
                            responses.onError(Status.UNAVAILABLE.withDescription("Injected transient failure").asRuntimeException());
                        } else if ((outcome == Outcome.PARTIAL_INTERNAL_ERROR || outcome == Outcome.PARTIAL_BACKPRESSURE)
                                && audited > 0) {
                            terminated = true;
                            responses.onNext(AuditorResponse.newBuilder()
                                    .setStatus(outcome == Outcome.PARTIAL_INTERNAL_ERROR ? "INTERNAL_ERROR" : "BACKPRESSURE_VIOLATION")
                                    .setStatusMessage("Injected backpressure violation").build());
                        } else if (outcome == Outcome.TOTAL_FAILURE
                                || (outcome == Outcome.PARTIAL_FAILURE || outcome == Outcome.EMPTY_AUDIT_FAILURE) && audited > 0) {
                            terminated = true;
                            responses.onError(Status.INVALID_ARGUMENT.withDescription("Injected terminal failure")
                                    .asRuntimeException());
                        } else {
                            audited++;
                            var response = AuditorResponse.newBuilder().setStatus("SUCCESS")
                                    .setRequestId(request.getAudit().getRequestId())
                                    .setIssueId(request.getAudit().getIssueData().getInstanceId());
                            if (outcome == Outcome.COMPLETED_WITH_ISSUE_FAILURE && audited == 2) {
                                response.setStatus("FAILED").setStatusMessage("Issue-level audit failure");
                            } else if (outcome != Outcome.EMPTY_AUDIT_FAILURE) {
                                response.setAuditResult(AuditResult.newBuilder()
                                        .setTagValue("Exploitable").setComment("Audit result"));
                            }
                            responses.onNext(response.build());
                        }
                    }
                }

                @Override public void onError(Throwable failure) { }
                @Override public void onCompleted() {
                    if (!terminated) {
                        responses.onCompleted();
                    }
                }
            };
        }
    }
}
