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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fortify.aviator.grpc.AuditResult;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator.audit.model.IssueData;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.common.fpr.model.StackTraceElement;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.SourceDecoders;
import com.fortify.cli.common.progress.helper.IProgressWriter;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;

@Timeout(15)
class AviatorStreamRecoveryTest {
    @TempDir
    Path tempDir;

    private static final IAviatorLogger LOGGER = new IAviatorLogger() {
        public void progress(String format, Object... args) {}
        public void info(String format, Object... args) {}
        public void warn(String format, Object... args) {}
        public void error(String format, Object... args) {}
    };

    @Test
    void recoveryRetryIsVisibleAsBoundedUserWarningWithoutServerDetails() throws Exception {
        var progressWriter = new CapturingProgressWriter();
        try (var fixture = new Fixture(1, new AviatorLoggerImpl(progressWriter))) {
            fixture.initialized().fail(Status.UNAVAILABLE.withDescription("private stream detail"));
            fixture.scheduler.awaitRetry();

            assertEquals(1, progressWriter.warnings.size());
            String warning = progressWriter.warnings.get(0);
            assertTrue(warning.startsWith("WARN: Retrying stream connection"), warning);
            assertFalse(warning.contains("150-second recovery budget"), warning);
            assertTrue(warning.contains("recovery attempt 1"), warning);
            assertFalse(warning.contains("private stream detail"), "Retry warnings must not expose transport details");
        }
    }

    @Test
    void successfulResultHalfClosesWithoutCancellingEvenWhenSendHasNotReturned() throws Exception {
        try (var fixture = new Fixture()) {
            var call = fixture.channel.nextCall();
            call.takeRequest();
            var releaseSend = new CountDownLatch(1);
            call.releaseAuditSend = releaseSend;
            try {
                call.respond("SUCCESS");
                call.takeRequest();
                call.auditSuccess();
                assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
                assertFalse(call.cancelled, "Successful cleanup must not race graceful half-close with abort");
            } finally {
                releaseSend.countDown();
            }
            call.halfClosed.get(5, TimeUnit.SECONDS);
            assertFalse(call.cancelled, "Verify before fixture.close()");
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void overdueTimerDoesNotAllowLateInitializationToSucceed(boolean initialize) throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var late = fixture.channel.nextCall();
            late.takeRequest();
            fixture.scheduler.elapseWithoutRunning(150_000);
            if (initialize) late.respond("SUCCESS");
            late.listener.onClose(Status.OK, new Metadata());
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertTrue(failed.failure().getMessage().contains("150 seconds"));
            late.awaitCancellation();
            assertTrue(late.requests.isEmpty(), "Overdue initialization must not submit an audit");
        }
    }

    @Test
    void overdueTimerDoesNotAdmitAnotherAttemptAfterDeadline() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            fixture.scheduler.awaitRetry();
            fixture.scheduler.elapseWithoutRunning(150_000);
            fixture.scheduler.runRetryWithoutTimers();
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertTrue(failed.failure().getMessage().contains("150 seconds"));
            assertTrue(fixture.channel.calls.isEmpty(), "Deadline is enforced without running the scheduled timeout");
        }
    }

    @Test
    void reconnectQuotaWaitsForOutstandingAuditAfterPriorSuccess() throws Exception {
        try (var fixture = new Fixture(3)) {
            var first = fixture.initialized();
            first.auditSuccess();
            first.takeRequest();
            first.takeRequest();
            first.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var retry = fixture.channel.nextCall();
            retry.takeRequest();
            retry.listener.onMessage(AuditorResponse.newBuilder().setStatus("SUCCESS").setQuota(1).build());
            assertEquals("issue-2", retry.takeRequest().getAudit().getIssueData().getInstanceId());
            assertThrows(TimeoutException.class, () -> fixture.result.get(200, TimeUnit.MILLISECONDS),
                    "One prior success cannot satisfy the one remaining filtered audit");
            retry.auditSuccess();
            var completed = assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1", "issue-2"), completed.responses().keySet());
            assertTrue(retry.requests.isEmpty(), "Quota-excluded work must not be sent");
        }
    }

    @Test
    void reconnectQuotaSkipDoesNotCompleteWhileAnotherAuditIsOutstanding() throws Exception {
        Path fpr = tempDir.resolve("missing-source.fpr");
        try (var zip = new ZipOutputStream(Files.newOutputStream(fpr))) {
            zip.putNextEntry(new ZipEntry("src-archive/index.xml"));
            zip.write(("<properties><entry key=\"missing.java\">src-archive/1</entry></properties>")
                    .getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        try (var handle = new FprHandle(fpr); var fixture = new Fixture(4, LOGGER, handle)) {
            var first = fixture.initialized();
            first.auditSuccess();
            first.takeRequest();
            first.takeRequest();
            first.takeRequest();
            fixture.prompts.stream().filter(prompt -> "issue-3".equals(prompt.getIssueData().getInstanceID()))
                    .findFirst().orElseThrow().getStackTrace().add(List.of(
                            new StackTraceElement("missing.java", 1, null, null, null, null, null)));
            first.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var retry = fixture.channel.nextCall();
            retry.takeRequest();
            retry.listener.onMessage(AuditorResponse.newBuilder().setStatus("SUCCESS").setQuota(2).build());
            assertEquals("issue-2", retry.takeRequest().getAudit().getIssueData().getInstanceId());
            assertThrows(TimeoutException.class, () -> fixture.result.get(200, TimeUnit.MILLISECONDS),
                    "Prior success plus a local skip cannot complete an outstanding filtered audit");
            retry.auditSuccess();
            var completed = assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1", "issue-2", "issue-3"), completed.responses().keySet());
            assertEquals("SKIPPED", completed.responses().get("issue-3").getStatus());
            assertTrue(retry.requests.isEmpty());
        }
    }

    @Test
    void applicationBusyDuringRecoveryWaitsForLeaseThenSucceeds() throws Exception {
        try (var fixture = new Fixture()) {
            var first = fixture.initialized();
            first.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var busy = fixture.channel.nextCall();
            busy.takeRequest();
            busy.respond("APPLICATION_BUSY");
            fixture.scheduler.nextRetry();
            var recovered = fixture.channel.nextCall();
            recovered.takeRequest();
            recovered.respond("SUCCESS");
            recovered.auditSuccess();
            assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
            first.awaitCancellation();
            busy.awaitCancellation();
            assertTrue(first.cancelled, "Failed attempt must be cancelled, not half-closed");
            assertTrue(busy.cancelled);
        }
    }

    @Test
    void recoveryIncludesSilentInitializationAndExpiresAtExactly150Seconds() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            fixture.scheduler.awaitTask(150_000);
            fixture.scheduler.nextRetry();
            var silent = fixture.channel.nextCall();
            silent.takeRequest();
            fixture.scheduler.awaitTask(30_000);
            fixture.scheduler.advanceTo(149_999);
            assertFalse(fixture.result.isDone(), "Recovery must not expire before the production 150 second budget");
            fixture.scheduler.advanceTo(150_000);
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertTrue(failed.failure().getMessage().contains("150 seconds"));
            silent.awaitCancellation();
            assertTrue(failed.responses().isEmpty());
        }
    }

    @Test
    void initializationTimeoutDuringRecoveryRetriesWithinTheSameBudget() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var silent = fixture.channel.nextCall();
            silent.takeRequest();
            fixture.scheduler.nextInitializationTimeout();
            silent.awaitCancellation();
            assertFalse(fixture.result.isDone());
            fixture.scheduler.nextRetry();
            var recovered = fixture.channel.nextCall();
            recovered.takeRequest();
            recovered.respond("SUCCESS");
            recovered.auditSuccess();
            assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void leaseRecoveryCanExceed120SecondsAndFiveAttemptsWithoutProgress() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            for (int retry = 0; retry < 7; retry++) {
                fixture.scheduler.nextRetry();
                var call = fixture.channel.nextCall();
                call.takeRequest();
                if (retry < 6) {
                    call.respond("APPLICATION_BUSY");
                } else {
                    assertTrue(fixture.scheduler.now >= 120_000);
                    assertTrue(fixture.scheduler.now < 150_000);
                    call.respond("SUCCESS");
                    call.auditSuccess();
                }
            }
            assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void acceptedInitializationEndsBudgetEvenWhileAuditIsSlowAndLaterDisconnectGetsFreshBudget() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var recovered = fixture.channel.nextCall();
            recovered.takeRequest();
            recovered.respond("SUCCESS");
            recovered.takeRequest();
            fixture.scheduler.advanceTo(300_000);
            assertFalse(fixture.result.isDone(), "An accepted stream has no per-model recovery deadline");
            recovered.fail(Status.UNAVAILABLE);
            fixture.scheduler.awaitTaskCount(150_000, 2);
            fixture.scheduler.nextRetry();
            var silent = fixture.channel.nextCall();
            silent.takeRequest();
            fixture.scheduler.awaitTaskCount(30_000, 3);
            fixture.scheduler.advanceTo(449_999);
            assertFalse(fixture.result.isDone(), "The later disconnect must get a fresh full episode");
            fixture.scheduler.advanceTo(450_000);
            assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "APPLICATION_BUSY_OTHER", "SERVER_BUSY", "UNAUTHENTICATED", "PERMISSION_DENIED", "QUOTA_EXCEEDED", "INVALID_ARGUMENT"})
    void permanentInitializationRejectionRemainsTerminal(String status) throws Exception {
        try (var fixture = new Fixture()) {
            var call = fixture.channel.nextCall();
            call.takeRequest();
            call.respond(status);
            assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            fixture.scheduler.advanceTo(300_000);
            assertTrue(fixture.channel.calls.isEmpty(), "Only exact APPLICATION_BUSY may retry initialization");
        }
    }

    @Test
    void staleCallbacksCannotCompleteOrFailTheReplacementAttempt() throws Exception {
        try (var fixture = new Fixture()) {
            var old = fixture.initialized();
            old.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var current = fixture.channel.nextCall();
            current.takeRequest();
            old.respond("SUCCESS");
            old.auditSuccess();
            old.respond("SERVER_BUSY");
            old.fail(Status.INVALID_ARGUMENT);
            old.listener.onClose(Status.OK, new Metadata());
            assertFalse(fixture.result.isDone());
            current.respond("SUCCESS");
            var request = current.takeRequest();
            assertTrue(request.hasAudit(), "Old initialization must not start replacement processing");
            assertEquals("issue-1", request.getAudit().getIssueData().getInstanceId());
            current.auditSuccess();
            var completed = assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1"), completed.responses().keySet());
            assertTrue(current.requests.isEmpty(), "A stale audit must not cause duplicate replay");
        }
    }

    @Test
    void closingDuringRecoveryCancelsDelayAndDoesNotOpenAnotherAttempt() throws Exception {
        try (var fixture = new Fixture()) {
            var call = fixture.initialized();
            call.fail(Status.UNAVAILABLE);
            fixture.scheduler.awaitRetry();
            fixture.processor.close();
            assertThrows(ExecutionException.class, () -> fixture.result.get(5, TimeUnit.SECONDS));
            fixture.scheduler.advanceTo(300_000);
            call.respond("SUCCESS");
            call.listener.onClose(Status.OK, new Metadata());
            assertTrue(fixture.channel.calls.isEmpty());
            call.awaitCancellation();
        }
    }

    @Test
    void protocolErrorsCannotRetryBeyondTheEpisodeBudget() throws Exception {
        try (var fixture = new Fixture()) {
            fixture.initialized().fail(Status.INTERNAL.withDescription("PROTOCOL_ERROR"));
            fixture.scheduler.awaitTask(150_000);
            fixture.scheduler.awaitRetry();
            fixture.scheduler.advanceTo(150_000);
            assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void expiryPreservesUsablePartialResultsAndDoesNotReplayReceivedAudits() throws Exception {
        try (var fixture = new Fixture(2)) {
            var first = fixture.initialized();
            first.auditSuccess();
            assertEquals("issue-2", first.takeRequest().getAudit().getIssueData().getInstanceId());
            first.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var retry = fixture.channel.nextCall();
            retry.takeRequest();
            retry.respond("SUCCESS");
            assertEquals("issue-2", retry.takeRequest().getAudit().getIssueData().getInstanceId());
            retry.fail(Status.UNAVAILABLE);
            long episodeStart = fixture.scheduler.now;
            fixture.scheduler.awaitTaskCount(150_000, 2);
            fixture.scheduler.awaitRetry();
            fixture.scheduler.advanceTo(episodeStart + 150_000);
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1"), failed.responses().keySet());
            assertEquals("SUCCESS", failed.responses().get("issue-1").getStatus());
            assertNotNull(failed.responses().get("issue-1").getAuditResult());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"UNAUTHENTICATED", "PERMISSION_DENIED", "INVALID_ARGUMENT", "RESOURCE_EXHAUSTED", "CANCELLED"})
    void permanentTransportFailureIsNotRetriedAfterPartialSuccess(String status) throws Exception {
        try (var fixture = new Fixture(2)) {
            var first = fixture.initialized();
            first.auditSuccess();
            first.takeRequest();
            first.fail(Status.fromCode(Status.Code.valueOf(status)));
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1"), failed.responses().keySet());
            fixture.scheduler.advanceTo(300_000);
            assertTrue(fixture.channel.calls.isEmpty());
        }
    }

    @Test
    void interruptionDuringRecoveryRemainsTerminal() throws Exception {
        try (var fixture = new Fixture()) {
            var call = fixture.initialized();
            call.fail(Status.UNAVAILABLE);
            fixture.scheduler.awaitRetry();
            fixture.caller.shutdownNow();
            assertThrows(ExecutionException.class, () -> fixture.result.get(5, TimeUnit.SECONDS));
            fixture.scheduler.advanceTo(300_000);
            assertTrue(fixture.channel.calls.isEmpty());
            call.awaitCancellation();
        }
    }

    @Test
    void permanentAsynchronousSendFailureIsNotReclassifiedAsTransient() throws Exception {
        try (var fixture = new Fixture()) {
            var call = fixture.channel.nextCall();
            call.takeRequest();
            call.sendFailure = Status.UNAUTHENTICATED.asRuntimeException();
            call.respond("SUCCESS");
            var failed = assertInstanceOf(SastStreamOutcome.Failed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Status.Code.UNAUTHENTICATED, Status.fromThrowable(failed.failure()).getCode());
            assertTrue(fixture.channel.calls.isEmpty());
        }
    }

    @Test
    void disconnectWhilePreparingServerBusyRetryDoesNotLoseOutstandingTracking() throws Exception {
        var preparing = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var preparations = new AtomicInteger();
        var logger = new IAviatorLogger() {
            public void progress(String format, Object... args) {}
            public void warn(String format, Object... args) {}
            public void error(String format, Object... args) {}
            public void info(String format, Object... args) {
                if ("Size of files {}".equals(format) && preparations.incrementAndGet() == 2) {
                    preparing.countDown();
                    try {
                        assertTrue(release.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        };
        try (var fixture = new Fixture(1, logger)) {
            var first = fixture.initialized();
            first.listener.onMessage(AuditorResponse.newBuilder().setStatus("SERVER_BUSY")
                    .setRequestId(first.lastAudit.getAudit().getRequestId()).build());
            assertTrue(preparing.await(5, TimeUnit.SECONDS));
            first.fail(Status.UNAVAILABLE);
            fixture.scheduler.nextRetry();
            var retry = fixture.channel.nextCall();
            retry.takeRequest();
            retry.respond("SUCCESS");
            retry.takeRequest();
            assertThrows(TimeoutException.class, () -> fixture.result.get(200, TimeUnit.MILLISECONDS),
                    "A replayed SERVER_BUSY request must still wait for its audit response");
            retry.auditSuccess();
            var completed = assertInstanceOf(SastStreamOutcome.Completed.class, fixture.result.get(5, TimeUnit.SECONDS));
            assertEquals(Set.of("issue-1"), completed.responses().keySet());
        } finally {
            release.countDown();
        }
    }

    private static final class Fixture implements AutoCloseable {
        final ManualScheduler scheduler = new ManualScheduler();
        final TestChannel channel = new TestChannel(scheduler);
        final ExecutorService workers = Executors.newFixedThreadPool(2);
        final ExecutorService caller = Executors.newSingleThreadExecutor();
        final AviatorGrpcClient client;
        final AviatorStreamProcessor processor;
        final CompletableFuture<SastStreamOutcome> result;
        final ConcurrentLinkedDeque<UserPrompt> prompts = new ConcurrentLinkedDeque<>();

        Fixture() { this(1); }

        Fixture(int count) { this(count, LOGGER); }

        Fixture(int count, IAviatorLogger logger) { this(count, logger, null); }

        Fixture(int count, IAviatorLogger logger, FprHandle handle) {
            client = new AviatorGrpcClient(
                    NettyChannelBuilder.forAddress("127.0.0.1", 1).usePlaintext().build(), 30, logger, 3600,
                    AviatorRequestPolicy.WAIT_FOR_READY, null);
            processor = new AviatorStreamProcessor(client, logger,
                    AuditorServiceGrpc.newStub(channel), workers, scheduler, 3600, 30, handle, SourceDecoders.defaults(), null,
                    () -> TimeUnit.MILLISECONDS.toNanos(scheduler.now));
            for (int i = 1; i <= count; i++) {
                prompts.add(UserPrompt.builder().issueData(IssueData.builder().instanceID("issue-" + i)
                        .accuracy("").confidence("").impact("").likelihood("").probability("").build())
                        .stackTrace(new ArrayList<>()).files(new ArrayList<>()).programmingLanguages(Set.of("java")).build());
            }

            result = CompletableFuture.supplyAsync(() -> processor.processBatchRequests(
                    prompts, "app", "build", "ssc-app", "version", "token", List.of()),
                    caller).thenCompose(future -> future);
        }

        TestCall initialized() throws Exception {
            var call = channel.nextCall();
            assertTrue(call.takeRequest().hasInit());
            call.respond("SUCCESS");
            assertTrue(call.takeRequest().hasAudit());
            return call;
        }

        public void close() {
            processor.close();
            caller.shutdownNow();
            workers.shutdownNow();
            scheduler.shutdownNow();
            client.close();
        }
    }

    private static final class CapturingProgressWriter implements IProgressWriter {
        private final List<String> warnings = new java.util.concurrent.CopyOnWriteArrayList<>();

        @Override public boolean isMultiLineSupported() { return false; }
        @Override public void writeProgress(String message, Object... args) {}
        @Override public void writeInfo(String message, Object... args) {}
        @Override public void writeInfoWithException(String message, Throwable cause, Object... args) {}
        @Override public void writeWarning(String message, Object... args) {
            warnings.add(args == null || args.length == 0 ? message : String.format(message, args));
        }
        @Override public void writeWarningWithException(String message, Throwable cause, Object... args) {}
        @Override public void clearProgress() {}
        @Override public void close() {}
        @Override public String type() { return "test"; }
    }

    private static final class ManualScheduler extends ScheduledThreadPoolExecutor {
        private record Task(long due, Runnable action, ScheduledFuture<?> future, boolean retry) {}
        private final List<Task> tasks = new ArrayList<>();
        private final List<Long> delays = new ArrayList<>();
        private volatile long now;

        ManualScheduler() { super(1); }

        @Override
        public synchronized ScheduledFuture<?> schedule(Runnable action, long delay, TimeUnit unit) {
            var future = super.schedule(() -> {}, 1, TimeUnit.DAYS);
            tasks.add(new Task(now + unit.toMillis(delay), action, future, unit == TimeUnit.MILLISECONDS));
            delays.add(unit.toMillis(delay));
            notifyAll();
            return future;
        }

        void elapseWithoutRunning(long target) { now = target; }

        void runRetryWithoutTimers() {
            Task retry;
            synchronized (this) {
                retry = tasks.stream().filter(task -> task.retry && !task.future.isCancelled()).findFirst().orElseThrow();
                tasks.remove(retry);
            }
            retry.action.run();
        }

        void nextRetry() throws Exception {
            long due;
            synchronized (this) {
                awaitRetry();
                due = tasks.stream().filter(item -> item.retry && !item.future.isCancelled())
                        .mapToLong(Task::due).min().orElseThrow();
            }
            advanceTo(due);
        }

        synchronized void awaitRetry() throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (tasks.stream().noneMatch(task -> task.retry && !task.future.isCancelled())) {
                long remaining = end - System.nanoTime();
                assertTrue(remaining > 0, "No recovery retry scheduled");
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
        }

        synchronized void awaitTask(long delay) throws Exception {
            awaitTaskCount(delay, 1);
        }

        synchronized void awaitTaskCount(long delay, int count) throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (delays.stream().filter(value -> value == delay).count() < count) {
                long remaining = end - System.nanoTime();
                assertTrue(remaining > 0, "No task scheduled for " + delay + " ms");
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }

        }

        synchronized void awaitInitialization() throws Exception {
            long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (tasks.stream().noneMatch(task -> !task.retry && !task.future.isCancelled() && task.due - now == 30_000)) {
                long remaining = end - System.nanoTime();
                assertTrue(remaining > 0, "Initialization timeout was not armed");
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
            }
        }

        void nextInitializationTimeout() throws Exception {
            long due;
            synchronized (this) {
                awaitTaskCount(30_000, 2);
                due = tasks.stream().filter(item -> !item.future.isCancelled() && item.due - now == 30_000)
                        .mapToLong(Task::due).findFirst().orElseThrow();
            }
            advanceTo(due);
        }

        void advanceTo(long target) {
            while (true) {
                Task next;
                synchronized (this) {
                    next = tasks.stream().filter(task -> !task.future.isCancelled() && task.due <= target)
                            .min(Comparator.comparingLong(Task::due)).orElse(null);
                    if (next == null) {
                        now = target;
                        return;
                    }
                    tasks.remove(next);
                    now = next.due;
                }
                next.action.run();
            }
        }
    }

    private static final class TestChannel extends Channel {
        private final LinkedBlockingQueue<TestCall> calls = new LinkedBlockingQueue<>();
        private final ManualScheduler scheduler;
        TestChannel(ManualScheduler scheduler) { this.scheduler = scheduler; }
        public String authority() { return "offline-test"; }

        @Override
        @SuppressWarnings("unchecked")
        public <ReqT, RespT> ClientCall<ReqT, RespT> newCall(MethodDescriptor<ReqT, RespT> method, CallOptions options) {
            return (ClientCall<ReqT, RespT>) new TestCall(calls, scheduler);
        }

        TestCall nextCall() throws Exception {
            var call = calls.poll(5, TimeUnit.SECONDS);
            assertNotNull(call, "Expected another transport attempt");
            return call;
        }
    }

    private static final class TestCall extends ClientCall<UserPromptRequest, AuditorResponse> {
        private final LinkedBlockingQueue<TestCall> calls;
        private final ManualScheduler scheduler;
        private final LinkedBlockingQueue<UserPromptRequest> requests = new LinkedBlockingQueue<>();
        private Listener<AuditorResponse> listener;
        private UserPromptRequest lastAudit;
        private final CompletableFuture<Void> cancellation = new CompletableFuture<>();
        private final CompletableFuture<Void> halfClosed = new CompletableFuture<>();
        private volatile boolean cancelled;
        private volatile RuntimeException sendFailure;
        private volatile CountDownLatch releaseAuditSend;

        TestCall(LinkedBlockingQueue<TestCall> calls, ManualScheduler scheduler) {
            this.calls = calls;
            this.scheduler = scheduler;
        }
        public void start(Listener<AuditorResponse> listener, Metadata headers) {
            this.listener = listener;
            calls.add(this);
        }
        public void request(int count) {}
        public void cancel(String message, Throwable cause) {
            cancelled = true;
            listener.onClose(Status.CANCELLED.withCause(cause), new Metadata());
            cancellation.complete(null);
        }
        public void halfClose() { halfClosed.complete(null); }
        public void sendMessage(UserPromptRequest request) {
            if (sendFailure != null) throw sendFailure;
            requests.add(request);
            if (request.hasAudit() && releaseAuditSend != null) {
                try {
                    assertTrue(releaseAuditSend.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
            }
        }
        void fail(Status status) { listener.onClose(status, new Metadata()); }
        void respond(String status) { listener.onMessage(AuditorResponse.newBuilder().setStatus(status).build()); }

        UserPromptRequest takeRequest() throws Exception {
            var request = requests.poll(5, TimeUnit.SECONDS);
            assertNotNull(request, "Expected a request");
            if (request.hasInit()) scheduler.awaitInitialization();
            if (request.hasAudit()) lastAudit = request;
            return request;
        }

        void auditSuccess() throws Exception {
            if (lastAudit == null) takeRequest();
            listener.onMessage(AuditorResponse.newBuilder().setStatus("SUCCESS")
                    .setRequestId(lastAudit.getAudit().getRequestId())
                    .setIssueId(lastAudit.getAudit().getIssueData().getInstanceId())
                    .setAuditResult(AuditResult.newBuilder().setTagValue("Exploitable").setComment("Received audit")).build());
        }

        void awaitCancellation() throws Exception { cancellation.get(5, TimeUnit.SECONDS); }
    }
}
