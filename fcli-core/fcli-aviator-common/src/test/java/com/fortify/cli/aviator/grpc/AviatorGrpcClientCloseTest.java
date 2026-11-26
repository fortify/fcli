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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.utils.SourceDecoders;

import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

class AviatorGrpcClientCloseTest {
    @Test
    @Timeout(20)
    void closeDuringRecoveryBackoffUnblocksPublicClientCall() throws Exception {
        var retryScheduled = new CountDownLatch(1);
        IAviatorLogger logger = new IAviatorLogger() {
            @Override public void progress(String format, Object... args) {}
            @Override public void info(String format, Object... args) {
                if (format.startsWith("WARN: Retrying stream connection after")) retryScheduled.countDown();
            }
            @Override public void warn(String format, Object... args) {
                if (format.startsWith("WARN: Retrying stream connection after")) retryScheduled.countDown();
            }
            @Override public void error(String format, Object... args) {}
        };
        var service = new AuditorServiceGrpc.AuditorServiceImplBase() {
            @Override
            public StreamObserver<UserPromptRequest> processStream(StreamObserver<AuditorResponse> responses) {
                return new StreamObserver<>() {
                    @Override
                    public void onNext(UserPromptRequest request) {
                        if (request.hasInit()) {
                            responses.onError(Status.UNAVAILABLE.withDescription("Injected disconnect").asRuntimeException());
                        }
                    }

                    @Override public void onError(Throwable error) {}
                    @Override public void onCompleted() {}
                };
            }
        };
        var server = NettyServerBuilder.forAddress(new InetSocketAddress("localhost", 0))
                .addService(service).build().start();
        var channel = NettyChannelBuilder.forAddress("localhost", server.getPort()).usePlaintext().build();
        var pingScheduler = new NonRunningScheduler();
        var client = new AviatorGrpcClient(channel, 30, logger, 3600, AviatorRequestPolicy.WAIT_FOR_READY, null, pingScheduler);
        ExecutorService caller = Executors.newSingleThreadExecutor();
        try {
            var call = CompletableFuture.supplyAsync(() -> client.processBatchRequests(
                    new ConcurrentLinkedDeque<>(List.of(prompt())), "app", "build", "ssc-app", "version",
                    "token", null, null, SourceDecoders.defaults(), new FVDLMetadata()), caller).thenCompose(future -> future);
            assertTrue(retryScheduled.await(5, TimeUnit.SECONDS), "Client did not enter stream recovery backoff");
            TimeUnit.MILLISECONDS.sleep(100);

            client.close();

            var failure = assertThrows(ExecutionException.class, () -> call.get(2, TimeUnit.SECONDS));
            assertInstanceOf(AviatorTechnicalException.class, failure.getCause());
            assertTrue(failure.getCause().getMessage().contains("Audit stream closed"));
        } finally {
            client.close();
            caller.shutdownNow();
            caller.awaitTermination(5, TimeUnit.SECONDS);
            server.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private static UserPrompt prompt() {
        return UserPrompt.builder().issueData(IssueData.builder().instanceID("issue-1")
                .accuracy("").confidence("").impact("").likelihood("").probability("").build())
                .stackTrace(new ArrayList<>()).files(new ArrayList<>()).programmingLanguages(Set.of("java")).build();
    }

    private static final class NonRunningScheduler extends java.util.concurrent.ScheduledThreadPoolExecutor {
        NonRunningScheduler() {
            super(1);
            setRemoveOnCancelPolicy(true);
        }

        @Override
        public java.util.concurrent.ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return super.schedule(() -> {}, 1, TimeUnit.DAYS);
        }
    }
}
