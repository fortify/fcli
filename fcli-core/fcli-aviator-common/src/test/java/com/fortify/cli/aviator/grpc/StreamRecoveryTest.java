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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.aviator.dastaudit.DastAuditClientMessage;
import com.fortify.aviator.dastaudit.DastAuditDecision;
import com.fortify.aviator.dastaudit.DastAuditErrorResponse;
import com.fortify.aviator.dastaudit.DastAuditInitResponse;
import com.fortify.aviator.dastaudit.DastAuditResponse;
import com.fortify.aviator.dastaudit.DastAuditServerMessage;
import com.fortify.aviator.dastaudit.DastAuditServiceGrpc;
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
import com.fortify.cli.common.fpr.dast.DastSession;

import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;

class StreamRecoveryTest {
    private static final String URL = "https://example.com/login";
    private static final IAviatorLogger LOGGER = new IAviatorLogger() {
        public void progress(String format, Object... args) { }
        public void info(String format, Object... args) { }
        public void warn(String format, Object... args) { }
        public void error(String format, Object... args) { }
    };

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void startingAfterPublicClientCloseFailsWithoutOpeningAnRpc(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var client = new AviatorGrpcClient(channel, 30, LOGGER, 0, AviatorRequestPolicy.WAIT_FOR_READY, null)) {
            client.close();
            try (var run = runWithClient(dast, client)) {
                assertThrows(CompletionException.class, run.future::join);
                assertEquals(0, channel.attemptCount(), "Registration after close must fail before opening a stream");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void registrationRacingPublicClientCloseCannotLeaveAPendingFuture(boolean dast) throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 5; i++) {
                var channel = new FakeChannel();
                try (var client = new AviatorGrpcClient(channel, 30, LOGGER, 0, AviatorRequestPolicy.WAIT_FOR_READY, null)) {
                    var gate = new CountDownLatch(1);
                    var starting = workers.submit(() -> {
                        gate.await();
                        return runWithClient(dast, client);
                    });
                    var closing = workers.submit(() -> {
                        gate.await();
                        client.close();
                        return null;
                    });
                    gate.countDown();
                    try (var run = starting.get(5, TimeUnit.SECONDS)) {
                        closing.get(8, TimeUnit.SECONDS);
                        assertTrue(run.future.isCompletedExceptionally());
                    }
                }
            }
        } finally {
            workers.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void futureCancellationRacingClientClosePreservesTerminalFailure(boolean dast) throws Exception {
        var workers = Executors.newFixedThreadPool(2);
        try {
            var channel = new FakeChannel();
            try (var client = new AviatorGrpcClient(channel, 30, LOGGER, 0, AviatorRequestPolicy.WAIT_FOR_READY, null);
                 var run = runWithClient(dast, client)) {
                channel.attempt(0).awaitMessages(1);
                var gate = new CountDownLatch(1);
                var cancelling = workers.submit(() -> {
                    gate.await();
                    run.future.cancel(true);
                    return null;
                });
                var closing = workers.submit(() -> {
                    gate.await();
                    client.close();
                    return null;
                });
                gate.countDown();
                cancelling.get(8, TimeUnit.SECONDS);
                closing.get(8, TimeUnit.SECONDS);
                assertTrue(run.future.isCompletedExceptionally());
                assertTrue(channel.attempt(0).cancelled);
                assertEquals(1, channel.attemptCount());
            }
        } finally {
            workers.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void publicClientCloseDuringBackoffCompletesTheProcessorFuture(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var client = new AviatorGrpcClient(channel, 30, LOGGER, 0, AviatorRequestPolicy.WAIT_FOR_READY, null);
             var run = runWithClient(dast, client)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.awaitMessages(2);
            var entered = new CountDownLatch(1);
            var release = new CountDownLatch(1);
            client.getPingScheduler().execute(() -> {
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            try {
                first.fail(Status.UNAVAILABLE);
                assertFalse(run.future.isDone());
                client.close();
                assertTrue(client.getPingScheduler().isShutdown());
                assertThrows(ExecutionException.class, () -> run.future.get(100, TimeUnit.MILLISECONDS));
                assertEquals(1, channel.attemptCount(), "Client shutdown must not reopen a retired RPC");
            } finally {
                release.countDown();
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void permanentRequestWriteFailureMustNotBeReclassifiedAsUnavailable(boolean dast) throws Exception {
        var channel = new FakeChannel();
        channel.failFirstAuditSend = true;
        channel.writeFailure = Status.PERMISSION_DENIED;
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            var failure = assertThrows(ExecutionException.class, () -> run.future.get(5, TimeUnit.SECONDS));
            assertEquals(Status.Code.PERMISSION_DENIED, Status.fromThrowable(failure.getCause()).getCode());
            scheduler.advance(300, TimeUnit.SECONDS);
            assertEquals(1, channel.attemptCount());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void duplicateErrorsForCompletedRequestsCannotFailRemainingWork(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler, 2)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            var requests = first.awaitMessages(3);
            first.respond(run.terminal(requests.get(1)));
            first.respond(run.error(requestId(requests.get(1)), "REQUEST_PROCESSING_ERROR", 0));
            assertFalse(run.future.isDone(), "A duplicate terminal response must not abort unrelated pending work");
            first.respond(run.terminal(requests.get(2)));
            first.complete();
            assertTrue(run.future.get(5, TimeUnit.SECONDS) != null);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void asynchronousRequestSendFailureRecoversWithoutLosingTheItem(boolean dast) throws Exception {
        var channel = new FakeChannel();
        channel.failFirstAuditSend = true;
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            scheduler.awaitTasks(3);
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(run.init());
            var replay = second.awaitMessages(2).get(1);
            second.respond(run.terminal(replay));
            second.complete();
            assertTrue(run.future.get(5, TimeUnit.SECONDS) != null);
            assertTrue(first.cancelled);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void failureAfterSomeAcceptedResultsStillFailsTheFuture(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler, 2)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            var pending = first.awaitMessages(3);
            first.respond(run.terminal(pending.get(1)));
            first.fail(Status.PERMISSION_DENIED);
            assertThrows(CompletionException.class, run.future::join);
            scheduler.advance(300, TimeUnit.SECONDS);
            assertEquals(1, channel.attemptCount());
        }
    }

    @Test
    void dastDisconnectAfterLastResultCompletesWithoutRetry() throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(true, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.respond(run.terminal(first.awaitMessages(2).get(1)));
            first.fail(Status.UNAVAILABLE);

            assertTrue(run.future.isDone(), "All terminal responses must remain available after a disconnect");
            var result = (DastAuditStreamResult) run.future.get(5, TimeUnit.SECONDS);
            assertEquals(1, result.results().size());
            assertEquals("DAST-0", result.results().get(0).issueId());
            assertEquals("SUCCESS", result.results().get(0).status());
            scheduler.advance(150, TimeUnit.SECONDS);
            assertEquals(1, channel.attemptCount(), "An empty stream must not be retried");
        }
    }

    @Test
    void dastPermanentFailureAfterLastResultRemainsExceptional() throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(true, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.respond(run.terminal(first.awaitMessages(2).get(1)));
            first.fail(Status.PERMISSION_DENIED);

            assertThrows(CompletionException.class, run.future::join);
            assertEquals(1, channel.attemptCount());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onlyExactApplicationBusyStatusCanRetryInitialization(boolean dast) throws Exception {
        for (var status : List.of("application_busy", "APPLICATION_LOCK_ERROR", "QUOTA_EXCEEDED", "INTERNAL_ERROR")) {
            var channel = new FakeChannel();
            try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
                channel.attempt(0).awaitMessages(1);
                channel.attempt(0).respond(run.error("", status, 0));
                assertThrows(CompletionException.class, run.future::join);
                scheduler.advance(300, TimeUnit.SECONDS);
                assertEquals(1, channel.attemptCount(), status);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void replayPreservesQueuedWorkWindowResultIdentityAndDastQuotaMetadata(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler, 12)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(dast ? DastAuditServerMessage.newBuilder().setInit(DastAuditInitResponse.newBuilder()
                .setStatus("SUCCESS").setReservedQuota(12).setExceededCount(1)
                .setQuotaLastUpdated("2026-01-01").setNextQuotaUpdateMessage("Previous update")).build()
                : run.windowInit(12));
            var sent = first.awaitMessages(11);
            var acceptedIds = new HashSet<String>();
            for (int i = 1; i <= 2; i++) {
                acceptedIds.add(requestId(sent.get(i)));
                first.respond(run.terminal(sent.get(i)));
            }
            first.awaitMessages(13);
            first.fail(Status.INTERNAL.withDescription("RST_STREAM closed"));
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(dast ? DastAuditServerMessage.newBuilder().setInit(DastAuditInitResponse.newBuilder()
                .setStatus("SUCCESS").setReservedQuota(10).setExceededCount(3)).build() : run.windowInit(10));
            var initialReplay = second.awaitMessages(11);
            assertEquals(11, initialReplay.size(), "Replay must respect the target's ten-request window");
            var replayedIds = new HashSet<String>();
            for (int i = 1; i <= 10; i++) {
                var request = second.awaitMessages(i + 1).get(i);
                assertFalse(acceptedIds.contains(requestId(request)), "Accepted work must not be replayed");
                assertTrue(replayedIds.add(requestId(request)), "Each unresolved item is submitted once per attempt");
                second.respond(run.terminal(request));
                second.respond(run.terminal(request));
            }
            first.respond(run.terminal(sent.get(3)));
            first.complete();
            second.complete();
            var result = run.future.get(5, TimeUnit.SECONDS);
            if (result instanceof DastAuditStreamResult audit) {
                assertEquals(12, audit.results().size());
                assertEquals(10, audit.reservedQuota(), "Quota metadata must describe the latest accepted stream");
                assertEquals(3, audit.exceededCount());
                assertFalse(audit.unlimitedQuota());
                assertNull(audit.quotaLastUpdated());
                assertNull(audit.nextQuotaUpdateMessage());
            } else {
                assertEquals(12, ((CorrelationResult) result).successfulCorrelationResponses());
            }
        }
    }

    @Test
    void dastReconnectReportsUnlimitedQuotaAndClearsPreviousMetadata() throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(true, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(DastAuditServerMessage.newBuilder().setInit(DastAuditInitResponse.newBuilder()
                .setStatus("SUCCESS").setReservedQuota(1)
                .setQuotaLastUpdated("2026-01-01").setNextQuotaUpdateMessage("Previous update")).build());
            first.awaitMessages(2);
            first.fail(Status.UNAVAILABLE);
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(run.init());
            second.respond(run.terminal(second.awaitMessages(2).get(1)));
            second.complete();

            var audit = (DastAuditStreamResult) run.future.get(5, TimeUnit.SECONDS);
            assertTrue(audit.unlimitedQuota());
            assertEquals(0, audit.reservedQuota());
            assertEquals(0, audit.exceededCount());
            assertNull(audit.quotaLastUpdated());
            assertNull(audit.nextQuotaUpdateMessage());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void firstInitializationTimeoutRemainsTerminal(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            channel.attempt(0).awaitMessages(1);
            scheduler.advance(29, TimeUnit.SECONDS);
            assertFalse(run.future.isDone());
            scheduler.advance(1, TimeUnit.SECONDS);
            var failure = assertThrows(CompletionException.class, run.future::join);
            assertTrue(failure.getCause().getMessage().contains("initialization timed out"));
            assertTrue(channel.attempt(0).cancelled);
            scheduler.advance(300, TimeUnit.SECONDS);
            assertEquals(1, channel.attemptCount());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void applicationBusyCanRecoverBeyondLeaseExpiryAndAcceptedInitResetsEpisode(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.error("", "APPLICATION_BUSY", 0));
            for (int i = 1; i <= 3; i++) {
                scheduler.advance(30, TimeUnit.SECONDS);
                var attempt = channel.attempt(i);
                attempt.awaitMessages(1);
                attempt.respond(run.error("", "APPLICATION_BUSY", 0));
            }
            scheduler.advance(40, TimeUnit.SECONDS);
            var recovered = channel.attempt(4);
            recovered.awaitMessages(1);
            recovered.respond(run.init());
            recovered.awaitMessages(2);
            scheduler.advance(200, TimeUnit.SECONDS);
            assertFalse(run.future.isDone());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void acceptedInitEndsRecoveryBeforeAnyResultAndLaterDisconnectGetsNewBudget(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.awaitMessages(2);
            first.fail(Status.UNAVAILABLE);
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(run.init());
            second.awaitMessages(2);
            scheduler.advance(200, TimeUnit.SECONDS);
            assertFalse(run.future.isDone(), "Accepted init ends recovery even before results arrive");
            second.fail(Status.UNAVAILABLE);
            scheduler.advance(149, TimeUnit.SECONDS);
            assertFalse(run.future.isDone(), "A later disconnect must have its own budget");
            scheduler.advance(1, TimeUnit.SECONDS);
            assertThrows(CompletionException.class, run.future::join);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void initAtExactDeadlineCannotWinEvenWhenScheduledExpiryIsDelayed(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.fail(Status.UNAVAILABLE);
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            scheduler.elapseWithoutRunning(140, TimeUnit.SECONDS);
            second.respond(run.init());
            assertThrows(CompletionException.class, run.future::join);
            assertEquals(1, second.messageCount(), "Do not submit work after the deadline");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void permanentTransportErrorsDoNotRetryOrReturnPartialSuccess(boolean dast) throws Exception {
        for (var status : List.of(Status.PERMISSION_DENIED, Status.UNAUTHENTICATED, Status.CANCELLED,
                Status.DEADLINE_EXCEEDED, Status.INVALID_ARGUMENT, Status.INTERNAL.withDescription("server failure"))) {
            var channel = new FakeChannel();
            try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
                var first = channel.attempt(0);
                first.awaitMessages(1);
                first.respond(run.init());
                first.awaitMessages(2);
                first.fail(status);
                assertThrows(CompletionException.class, run.future::join);
                scheduler.advance(300, TimeUnit.SECONDS);
                assertEquals(1, channel.attemptCount(), status.toString());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void earlyCompletionRecoversButGracefulCompletionReturnsTerminalResults(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.awaitMessages(2);
            first.complete();
            assertFalse(run.future.isDone(), "Incomplete server completion is not successful output");
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(run.init());
            var pending = second.awaitMessages(2).get(1);
            second.respond(run.terminal(pending));
            second.complete();
            assertTrue(run.future.get(5, TimeUnit.SECONDS) != null);
            scheduler.advance(300, TimeUnit.SECONDS);
            assertEquals(2, channel.attemptCount());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void closeAndCancellationDuringRecoveryRetireCallbacksAndDoNotReopen(boolean dast) throws Exception {
        for (boolean cancelFuture : List.of(false, true)) {
            var channel = new FakeChannel();
            try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
                var first = channel.attempt(0);
                first.awaitMessages(1);
                first.fail(Status.UNAVAILABLE);
                if (cancelFuture) run.future.cancel(true);
                else run.close();
                assertTrue(run.future.isCompletedExceptionally());
                first.respond(run.init());
                first.complete();
                scheduler.advance(300, TimeUnit.SECONDS);
                assertEquals(1, channel.attemptCount());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void requestBusyBackoffIsIndependentOfConnectionRecoveryBudget(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            var pending = first.awaitMessages(2).get(1);
            first.respond(run.error(requestId(pending), "SERVER_BUSY", 0));
            scheduler.advance(150, TimeUnit.SECONDS);
            assertFalse(run.future.isDone());
            assertEquals(1, channel.attemptCount());
            scheduler.advance(12, TimeUnit.SECONDS);
            var retried = first.awaitMessages(3).get(2);
            assertEquals(pending, retried);
            first.respond(run.terminal(retried));
            first.complete();
            assertTrue(run.future.get(5, TimeUnit.SECONDS) != null);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recoveryDeadlineIncludesBackoffAndSilentReinitialization(boolean dast) throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var run = run(dast, channel, scheduler)) {
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(run.init());
            first.awaitMessages(2);
            first.fail(Status.UNAVAILABLE);
            scheduler.advance(149, TimeUnit.SECONDS);
            assertFalse(run.future.isDone(), "Recovery init timeouts must not terminate the episode early");
            scheduler.advance(1, TimeUnit.SECONDS);
            var failure = assertThrows(CompletionException.class, run.future::join);
            assertTrue(failure.getCause().getMessage().contains("connection recovery timed out"));
            int attempts = channel.attemptCount();
            scheduler.advance(300, TimeUnit.SECONDS);
            assertEquals(attempts, channel.attemptCount(), "No retry may outlive the terminal deadline");
        }
    }

    @Test
    void validationRecoveryKeepsQuotaInitializationAndDoesNotRepeatCorrelation() throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler(); var processor = correlation(channel, scheduler)) {
            var future = processor.processCorrelation(config(), List.of(bucket(2)), "scan");
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(init());
            var requests = first.awaitMessages(3);
            for (int i = 1; i <= 2; i++) {
                var response = correlated((CorrelationClientMessage) requests.get(i));
                first.respond(response.toBuilder().setCorrelation(response.getCorrelation().toBuilder()
                    .addMatches(CorrelationCandidateMatch.newBuilder().setUrl(URL))).build());
            }
            var validations = first.awaitMessages(5);
            var accepted = (CorrelationClientMessage) validations.get(3);
            var pending = (CorrelationClientMessage) validations.get(4);
            first.respond(validated(accepted));
            first.fail(Status.UNAVAILABLE);
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            var restartInit = (CorrelationClientMessage) second.awaitMessages(1).get(0);
            assertEquals(2, restartInit.getInit().getTotalIssuesToCorrelate(),
                "The current server rejects a zero-quota initialization, including during validation");
            second.respond(init());
            var replay = (CorrelationClientMessage) second.awaitMessages(2).get(1);
            assertTrue(replay.hasValidation(), "Do not return to correlation after phase transition");
            assertEquals(pending.getValidation().getRequestId(), replay.getValidation().getRequestId());
            assertNotEquals(pending.getValidation().getStreamId(), replay.getValidation().getStreamId());
            first.respond(validated(pending));
            second.respond(validated(replay));
            second.respond(validated(replay));
            var result = future.get(5, TimeUnit.SECONDS);
            assertEquals(2, result.successfulCorrelationResponses());
            assertEquals(2, result.confirmedPairs().size());
            assertEquals(2, second.messageCount());
        }
    }

    @Test
    void correlationReplaysOnlyUnresolvedRequestsAndKeepsAcceptedResults() throws Exception {
        var channel = new FakeChannel();
        try (var scheduler = new ManualScheduler();
             var processor = correlation(channel, scheduler)) {
            var future = processor.processCorrelation(config(), List.of(bucket(2)), "scan");
            var first = channel.attempt(0);
            first.awaitMessages(1);
            first.respond(init());
            var requests = first.awaitMessages(3);
            var accepted = (CorrelationClientMessage) requests.get(1);
            var pending = (CorrelationClientMessage) requests.get(2);
            first.respond(correlated(accepted));
            first.fail(Status.UNAVAILABLE);

            assertFalse(future.isDone(), "A disconnect must start recovery, not fail the run");
            scheduler.advance(10, TimeUnit.SECONDS);
            var second = channel.attempt(1);
            second.awaitMessages(1);
            second.respond(init());
            var replay = (CorrelationClientMessage) second.awaitMessages(2).get(1);
            assertEquals(pending.getCorrelation().getRequestId(), replay.getCorrelation().getRequestId());
            assertNotEquals(pending.getCorrelation().getStreamId(), replay.getCorrelation().getStreamId());
            first.respond(correlated(pending));
            first.fail(Status.PERMISSION_DENIED);
            assertFalse(future.isDone(), "Retired stream callbacks must not mutate the active run");
            second.respond(correlated(replay));
            assertEquals(2, future.get(5, TimeUnit.SECONDS).successfulCorrelationResponses());
            assertEquals(2, second.messageCount(), "Accepted work must not be resubmitted");
        }
    }

    private static CorrelationStreamProcessor correlation(FakeChannel channel, ManualScheduler scheduler) {
        return new CorrelationStreamProcessor(null, LOGGER, CorrelationServiceGrpc.newStub(channel),
            scheduler, 0, 0, scheduler::nanoTime);
    }

    private static Run run(boolean dast, FakeChannel channel, ManualScheduler scheduler) {
        return run(dast, channel, scheduler, 1);
    }

    private static Run runWithClient(boolean dast, AviatorGrpcClient client) {
        if (!dast) {
            var processor = new CorrelationStreamProcessor(client, LOGGER, client.getCorrelationAsyncStub(),
                client.getPingScheduler(), 0, 30);
            return new Run(processor, processor.processCorrelation(config(), List.of(bucket(1)), "scan"), false);
        }
        var issue = new DastIssue();
        issue.setId("DAST-1");
        var processor = new DastAuditStreamProcessor(LOGGER, client.getDastAuditAsyncStub(), client.getPingScheduler(), 0);
        return new Run(processor, processor.process(DastAuditStreamConfig.builder().token("t").applicationName("app").build(),
            List.of(new DastAuditWorkItem(new DastSession(), issue)), 1), true);
    }

    private static Run run(boolean dast, FakeChannel channel, ManualScheduler scheduler, int count) {
        if (!dast) {
            var processor = correlation(channel, scheduler);
            return new Run(processor, processor.processCorrelation(config(), List.of(bucket(count)), "scan"), false);
        }
        var work = new ArrayList<DastAuditWorkItem>();
        for (int i = 0; i < count; i++) {
            var issue = new DastIssue();
            issue.setId("DAST-" + i);
            work.add(new DastAuditWorkItem(new DastSession(), issue));
        }
        var processor = new DastAuditStreamProcessor(LOGGER, DastAuditServiceGrpc.newStub(channel),
            scheduler, 0, scheduler::nanoTime);
        return new Run(processor, processor.process(DastAuditStreamConfig.builder().token("t").applicationName("app").build(),
            work, count), true);
    }

    private record Run(AutoCloseable processor, CompletableFuture<?> future, boolean dast) implements AutoCloseable {
        Object init() {
            return dast ? DastAuditServerMessage.newBuilder().setInit(
                DastAuditInitResponse.newBuilder().setStatus("SUCCESS").setUnlimitedQuota(true)).build()
                : StreamRecoveryTest.init();
        }
        Object windowInit(int quota) {
            return dast ? DastAuditServerMessage.newBuilder().setInit(DastAuditInitResponse.newBuilder()
                .setStatus("SUCCESS").setReservedQuota(quota).setExceededCount(1)).build()
                : CorrelationServerMessage.newBuilder().setInit(CorrelationInitResponse.newBuilder()
                    .setStatus("SUCCESS").setReservedQuota(quota)).build();
        }
        Object error(String id, String status, long retryAfterMs) {
            return dast ? DastAuditServerMessage.newBuilder().setError(DastAuditErrorResponse.newBuilder()
                .setRequestId(id).setStatus(status)).build()
                : CorrelationServerMessage.newBuilder().setError(CorrelationErrorResponse.newBuilder()
                    .setRequestId(id).setStatus(status)).build();
        }
        Object terminal(Object request) {
            return dast ? DastAuditServerMessage.newBuilder().setAudit(DastAuditResponse.newBuilder()
                .setRequestId(requestId(request)).setStatus("SUCCESS")
                .setDecision(DastAuditDecision.newBuilder().setConfidence("HIGH"))).build()
                : correlated((CorrelationClientMessage) request);
        }
        public void close() throws Exception { processor.close(); }
    }

    private static String requestId(Object message) {
        return message instanceof DastAuditClientMessage dast ? dast.getAudit().getRequestId()
            : ((CorrelationClientMessage) message).getCorrelation().getRequestId();
    }

    private static CorrelationStreamConfig config() {
        return new CorrelationStreamConfig("t", "app", null, null, null);
    }

    private static CorrelationStreamProcessor.CorrelationBucketData bucket(int count) {
        List<Vulnerability> sast = new ArrayList<>();
        for (int i = 0; i < count; i++) sast.add(Vulnerability.builder().instanceID("SAST-" + i).build());
        var dast = new DastIssue();
        dast.setId("DAST-1");
        dast.setSessionUrl(URL);
        return new CorrelationStreamProcessor.CorrelationBucketData("SQL Injection", sast, List.of(dast));
    }

    private static CorrelationServerMessage init() {
        return CorrelationServerMessage.newBuilder().setInit(
            CorrelationInitResponse.newBuilder().setStatus("SUCCESS").setUnlimitedQuota(true)).build();
    }

    private static CorrelationServerMessage correlated(CorrelationClientMessage request) {
        return CorrelationServerMessage.newBuilder().setCorrelation(CorrelationResponse.newBuilder()
            .setRequestId(request.getCorrelation().getRequestId())
            .setSastId(request.getCorrelation().getSastFinding().getInstanceId()).setStatus("SUCCESS")).build();
    }

    private static CorrelationServerMessage validated(CorrelationClientMessage request) {
        return CorrelationServerMessage.newBuilder().setValidation(CorrelationValidationResponse.newBuilder()
            .setRequestId(request.getValidation().getRequestId())
            .setSastId(request.getValidation().getSastFinding().getInstanceId())
            .setDecision(CorrelationDecision.newBuilder().setConfirmed(true)).setStatus("SUCCESS")).build();
    }

    private static class FakeChannel extends ManagedChannel {
        private final List<Attempt> attempts = new ArrayList<>();
        private boolean failFirstAuditSend;
        private Status writeFailure = Status.UNAVAILABLE.withDescription("failed write");
        private volatile boolean shutdown;
        public ManagedChannel shutdown() { shutdown = true; return this; }
        public ManagedChannel shutdownNow() { return shutdown(); }
        public boolean isShutdown() { return shutdown; }
        public boolean isTerminated() { return shutdown; }
        public boolean awaitTermination(long timeout, TimeUnit unit) { return shutdown; }
        public String authority() { return "fake"; }

        public synchronized <ReqT, RespT> ClientCall<ReqT, RespT> newCall(
                MethodDescriptor<ReqT, RespT> method, CallOptions options) {
            var attempt = new Attempt();
            attempts.add(attempt);
            return new ClientCall<>() {
                @SuppressWarnings("unchecked")
                public void start(Listener<RespT> listener, Metadata headers) {
                    attempt.listener = (Listener<Object>) listener;
                }
                public void request(int count) { }
                public void cancel(String message, Throwable cause) {
                    attempt.cancelled = true;
                    attempt.fail(Status.CANCELLED.withCause(cause));
                }
                public void halfClose() { attempt.halfClosed = true; }
                public void sendMessage(ReqT message) {
                    synchronized (attempt) {
                        if (failFirstAuditSend && attempt == attempts.get(0) && !attempt.messages.isEmpty()) {
                            throw writeFailure.asRuntimeException();
                        }
                        attempt.messages.add(message);
                        attempt.notifyAll();
                    }
                }
            };
        }

        synchronized Attempt attempt(int index) {
            assertTrue(attempts.size() > index, "Expected RPC attempt " + index);
            return attempts.get(index);
        }
        synchronized int attemptCount() { return attempts.size(); }
    }

    private static class Attempt {
        private ClientCall.Listener<Object> listener;
        private final List<Object> messages = new ArrayList<>();
        private volatile boolean cancelled;
        private volatile boolean halfClosed;

        synchronized List<Object> awaitMessages(int count) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (messages.size() < count && System.nanoTime() < deadline) {
                TimeUnit.NANOSECONDS.timedWait(this, Math.max(1, deadline - System.nanoTime()));
            }
            assertTrue(messages.size() >= count, "Expected " + count + " messages, got " + messages.size());
            return List.copyOf(messages);
        }
        synchronized int messageCount() { return messages.size(); }
        void respond(Object message) { listener.onMessage(message); }
        void fail(Status status) { listener.onClose(status, new Metadata()); }
        void complete() { listener.onClose(Status.OK, new Metadata()); }
    }

    private static class ManualScheduler extends ScheduledThreadPoolExecutor implements AutoCloseable {
        private final List<Task> tasks = new ArrayList<>();
        private long now;
        ManualScheduler() { super(1); }

        @Override
        public synchronized ScheduledFuture<?> schedule(Runnable action, long delay, TimeUnit unit) {
            var task = new Task(action, now + unit.toNanos(delay));
            tasks.add(task);
            notifyAll();
            return task;
        }

        synchronized void awaitTasks(int count) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (tasks.size() < count && System.nanoTime() < deadline) {
                TimeUnit.NANOSECONDS.timedWait(this, Math.max(1, deadline - System.nanoTime()));
            }
            assertTrue(tasks.size() >= count, "Expected recovery to be scheduled after asynchronous send failure");
        }

        void advance(long amount, TimeUnit unit) {
            long target = now + unit.toNanos(amount);
            while (true) {
                Task next;
                synchronized (this) {
                    next = tasks.stream().filter(t -> !t.isDone() && t.due <= target)
                        .min((a, b) -> Long.compare(a.due, b.due)).orElse(null);
                    if (next == null) { now = target; return; }
                    now = next.due;
                }
                next.run();
            }
        }
        synchronized long nanoTime() { return now; }
        synchronized void elapseWithoutRunning(long amount, TimeUnit unit) { now += unit.toNanos(amount); }
        public void close() { shutdownNow(); }

        private class Task extends FutureTask<Void> implements ScheduledFuture<Void> {
            private final long due;
            Task(Runnable action, long due) { super(action, null); this.due = due; }
            public long getDelay(TimeUnit unit) { return unit.convert(due - now, TimeUnit.NANOSECONDS); }
            public int compareTo(Delayed other) {
                return Long.compare(getDelay(TimeUnit.NANOSECONDS), other.getDelay(TimeUnit.NANOSECONDS));
            }
        }
    }
}
