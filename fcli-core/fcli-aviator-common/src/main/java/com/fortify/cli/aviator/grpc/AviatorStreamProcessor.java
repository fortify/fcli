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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.grpc.AuditRequest;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.PingRequest;
import com.fortify.aviator.grpc.StreamInitRequest;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator._common.exception.AviatorQuotaFilterException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.QuotaBasedFilter;
import com.fortify.cli.aviator.audit.SourceCodeEnricher;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.fpr.model.FVDLMetadata;
import com.fortify.cli.common.fpr.model.File;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.util.StringUtil;
import com.fortify.cli.common.fpr.utils.ISourceDecoder;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;

/**
 * SAST streaming with a total 150 second connection recovery budget per outage, ending at accepted initialization.
 * The ordinary initial initialization timeout and per-request SERVER_BUSY policy are independent of recovery.
 */
class AviatorStreamProcessor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorStreamProcessor.class);
    private static final long RECOVERY_TIMEOUT_SECONDS = 150;

    private final AviatorGrpcClient client;
    private final IAviatorLogger logger;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final ExecutorService processingExecutor;
    private final ScheduledExecutorService pingScheduler;
    private final long pingIntervalSeconds;
    private final long defaultTimeoutSeconds;
    private final LongSupplier nanoTime;

    private final Map<String, RequestMetrics> requestMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, RequestWrapper> inflightRequests = new ConcurrentHashMap<>();

    private final AtomicInteger consecutiveBackpressureViolations = new AtomicInteger(0);
    private final AtomicLong lastBackpressureViolation = new AtomicLong(0);
    private final AtomicInteger currentBackoff = new AtomicInteger(1);
    private final AtomicInteger serverWindowSize = new AtomicInteger(Constants.INITIAL_REQUEST_WINDOW);

    private final Semaphore requestSemaphore = new Semaphore(Constants.INITIAL_REQUEST_WINDOW);
    private final AtomicInteger outstandingRequests = new AtomicInteger(0);
    private RequestHandler<UserPromptRequest> requestHandler;

    private ScheduledFuture<?> pingTask;
    private final AtomicBoolean isPinging = new AtomicBoolean(false);

    private final ConcurrentLinkedDeque<RequestWrapper> processingQueue = new ConcurrentLinkedDeque<>();
    private volatile StreamState currentStreamState;
    private final Object retryLock = new Object();
    private Attempt activeAttempt;
    private CompletableFuture<SastStreamOutcome> activeResult;
    private ScheduledFuture<?> recoveryTimeout;
    private ScheduledFuture<?> retryDelay;
    private CountDownLatch retryDelayLatch;
    private Object recoveryEpisode;
    private long recoveryDeadlineNanos;
    private boolean closed;
    private final FprHandle fprHandle;
    private final ISourceDecoder sourceDecoder;
    private final FVDLMetadata fvdlMetadata;
    private final SourceCodeEnricher sourceCodeEnricher;
    private final AtomicInteger skippedRequests = new AtomicInteger();
    private final AtomicInteger failedRequests = new AtomicInteger();

    private static final class Attempt {
        final RequestHandler<UserPromptRequest> handler;
        final CountDownLatch done = new CountDownLatch(1);
        final CountDownLatch initialized = new CountDownLatch(1);
        StreamInitializationTimeout timeout;
        Future<?> processingTask;
        RequestWrapper preparing;
        boolean finished;

        Attempt(String streamId) {
            handler = new RequestHandler<>(streamId);
        }
    }

    public AviatorStreamProcessor(AviatorGrpcClient client, IAviatorLogger logger, AuditorServiceGrpc.AuditorServiceStub asyncStub,
            ExecutorService processingExecutor, ScheduledExecutorService pingScheduler, long pingIntervalSeconds,
            long defaultTimeoutSeconds, FprHandle fprHandle, ISourceDecoder sourceDecoder,
            FVDLMetadata fvdlMetadata) {
        this(client, logger, asyncStub, processingExecutor, pingScheduler, pingIntervalSeconds, defaultTimeoutSeconds,
                fprHandle, sourceDecoder, fvdlMetadata, System::nanoTime);
    }

    AviatorStreamProcessor(AviatorGrpcClient client, IAviatorLogger logger, AuditorServiceGrpc.AuditorServiceStub asyncStub,
            ExecutorService processingExecutor, ScheduledExecutorService pingScheduler, long pingIntervalSeconds,
            long defaultTimeoutSeconds, FprHandle fprHandle, ISourceDecoder sourceDecoder,
            FVDLMetadata fvdlMetadata, LongSupplier nanoTime) {
        this.client = client;
        this.logger = logger;
        this.asyncStub = asyncStub;
        this.processingExecutor = processingExecutor;
        this.pingScheduler = pingScheduler;
        this.pingIntervalSeconds = pingIntervalSeconds;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.nanoTime = Objects.requireNonNull(nanoTime, "nanoTime");
        this.fprHandle = fprHandle;
        this.sourceDecoder = Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        this.fvdlMetadata = fvdlMetadata;
        this.sourceCodeEnricher = new SourceCodeEnricher(fprHandle, this.sourceDecoder, fvdlMetadata);
    }

    public CompletableFuture<SastStreamOutcome> processBatchRequests(Queue<UserPrompt> requests, String projectName,
            String FPRBuildId, String SSCApplicationName, String SSCApplicationVersion, String token,
            List<String> customPriorityOrder) {
        if (requests == null || requests.isEmpty()) {
            LOG.info("No issues to process");
            return CompletableFuture.completedFuture(new SastStreamOutcome.Completed(Map.of()));
        }

        String streamId = UUID.randomUUID().toString();
        currentStreamState = new StreamState(streamId, projectName, FPRBuildId, SSCApplicationName, SSCApplicationVersion, token, requests.size());
        currentStreamState.setCustomPriorityOrder(customPriorityOrder);

        requests.stream().map(RequestWrapper::new).forEach(wrapper -> {
            this.processingQueue.add(wrapper);
            currentStreamState.pendingIssueIds.add(wrapper.userPrompt.getIssueData().getInstanceID());
        });

        final int totalRequests = processingQueue.size();
        logger.info("Starting processing - Total Issues: " + totalRequests);
        final CompletableFuture<SastStreamOutcome> resultFuture = new CompletableFuture<>();
        final Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        final AtomicInteger processedRequests = new AtomicInteger(0);

        activeResult = resultFuture;
        resultFuture.whenComplete((result, error) -> {
            synchronized (retryLock) {
                endRecovery();
                if (retryDelay != null) retryDelay.cancel(false);
                if (retryDelayLatch != null) retryDelayLatch.countDown();
                if (activeAttempt != null) {
                    retireAttempt(activeAttempt, error == null && result instanceof SastStreamOutcome.Completed);
                }
            }
        });
        startStreamWithRetry(responses, processedRequests, resultFuture);

        return resultFuture.exceptionally(ex -> {
            stopPingPong();
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) && ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof AviatorSimpleException) throw (AviatorSimpleException) cause;
            if (cause instanceof AviatorTechnicalException) throw (AviatorTechnicalException) cause;
            throw new AviatorTechnicalException("Processing FPR failed", cause);
        });
    }

    private void startStreamWithRetry(Map<String, AuditResponse> responses, AtomicInteger processedRequests,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        try {
            while (!resultFuture.isDone()) {
                CountDownLatch delayLatch = null;
                synchronized (retryLock) {
                    if (resultFuture.isDone()) return;
                    if (expireRecoveryIfOverdue(responses, resultFuture)) return;
                    if (closed || client.isShutdown.get()) {
                        resultFuture.completeExceptionally(new AviatorTechnicalException("Audit stream closed"));
                        return;
                    }
                    if (currentStreamState.streamRetryCount > 0) {
                        long delay = calculateStreamRetryDelay(currentStreamState.streamRetryCount);
                        logger.warn("WARN: Retrying stream connection after %d ms delay (recovery attempt %d)",
                                delay, currentStreamState.streamRetryCount);
                        delayLatch = new CountDownLatch(1);
                        retryDelayLatch = delayLatch;
                        retryDelay = pingScheduler.schedule(delayLatch::countDown, delay, TimeUnit.MILLISECONDS);
                    }
                }
                if (delayLatch != null) delayLatch.await();

                Attempt attempt;
                synchronized (retryLock) {
                    if (resultFuture.isDone() || closed) return;
                    if (expireRecoveryIfOverdue(responses, resultFuture)) return;
                    currentStreamState.streamRetryCount++;
                    currentStreamState.streamId = UUID.randomUUID().toString();
                    currentStreamState.isStreamInitialized = false;
                    outstandingRequests.set(0);
                    requestSemaphore.drainPermits();
                    requestSemaphore.release(Constants.INITIAL_REQUEST_WINDOW);
                    attempt = new Attempt(currentStreamState.streamId);
                    activeAttempt = attempt;
                    requestHandler = attempt.handler;
                    attempt.handler.getSendFailure().thenAccept(error -> {
                        synchronized (retryLock) {
                            failAttempt(attempt, error, isRetryableError(error), responses, resultFuture);
                        }
                    });
                    attempt.timeout = new StreamInitializationTimeout(pingScheduler, () -> {
                        synchronized (retryLock) {
                            failAttempt(attempt, StreamInitializationTimeout.expiredError("Audit"),
                                    recoveryEpisode != null, responses, resultFuture);
                        }
                    });
                }
                try {
                    asyncStub.processStream(createResponseObserver(responses, processedRequests, resultFuture, attempt));
                    synchronized (retryLock) {
                        if (isActive(attempt, resultFuture) && !expireRecoveryIfOverdue(responses, resultFuture)) {
                            sendInitRequest(attempt, responses, resultFuture);
                            attempt.timeout.arm();
                            attempt.processingTask = processingExecutor.submit(
                                    () -> processAttempt(attempt, responses, processedRequests, resultFuture));
                        }
                    }
                } catch (Exception e) {
                    synchronized (retryLock) {
                        failAttempt(attempt, e, isRetryableError(e), responses, resultFuture);
                    }
                }
                attempt.done.await();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            resultFuture.completeExceptionally(new AviatorTechnicalException("Interrupted during stream recovery", e));
        } catch (Exception e) {
            resultFuture.completeExceptionally(new AviatorTechnicalException("Error initiating batch processing", e));
        }
    }

    private boolean isActive(Attempt attempt, CompletableFuture<SastStreamOutcome> resultFuture) {
        return activeAttempt == attempt && !attempt.finished && !closed && !resultFuture.isDone();
    }

    private void processAttempt(Attempt attempt, Map<String, AuditResponse> responses, AtomicInteger processedRequests,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        try {
            attempt.initialized.await();
            synchronized (retryLock) {
                if (!isActive(attempt, resultFuture) || !currentStreamState.isStreamInitialized) return;
                applyQuotaFilteringIfNeeded(processedRequests.get());
            }
            processRequestQueue(currentStreamState.actualIssuesCount, processedRequests, responses, resultFuture, attempt);
        } catch (Exception e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            synchronized (retryLock) {
                if (!isActive(attempt, resultFuture)) return;
                resultFuture.completeExceptionally(e instanceof AviatorQuotaFilterException ? e
                        : new AviatorTechnicalException("Error during request processing execution", e));
            }
        }
    }

    private void retireAttempt(Attempt attempt, boolean graceful) {
        if (attempt.finished) return;
        attempt.finished = true;
        attempt.timeout.close();
        attempt.initialized.countDown();
        attempt.done.countDown();
        if (attempt.processingTask != null) attempt.processingTask.cancel(true);
        stopPingPong();
        if (graceful) {
            attempt.handler.complete();
        } else {
            attempt.handler.abort(new AviatorTechnicalException("Audit stream attempt closed"));
        }
    }

    private void failAttempt(Attempt attempt, Throwable error, boolean retryable, Map<String, AuditResponse> responses,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        if (!isActive(attempt, resultFuture)) return;
        if (expireRecoveryIfOverdue(responses, resultFuture)) return;
        if (!retryable) {
            var failure = error instanceof AviatorSimpleException simple ? simple
                    : new AviatorTechnicalException("gRPC stream failed: " + error.getMessage(), error);
            if (error instanceof StatusRuntimeException || error instanceof AviatorSimpleException) {
                resultFuture.complete(new SastStreamOutcome.Failed(responses, failure));
            } else {
                resultFuture.completeExceptionally(failure);
            }
            return;
        }
        if (recoveryEpisode == null) {
            Object episode = new Object();
            recoveryEpisode = episode;
            recoveryDeadlineNanos = nanoTime.getAsLong() + TimeUnit.SECONDS.toNanos(RECOVERY_TIMEOUT_SECONDS);
            currentStreamState.streamRetryCount = 1;
            recoveryTimeout = pingScheduler.schedule(() -> {
                synchronized (retryLock) {
                    if (recoveryEpisode != episode || resultFuture.isDone()) return;
                    resultFuture.complete(new SastStreamOutcome.Failed(responses,
                            new AviatorTechnicalException("Audit connection recovery timed out after 150 seconds", error)));
                }
            }, RECOVERY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        retireAttempt(attempt, false);
        if (attempt.preparing != null) {
            processingQueue.addFirst(attempt.preparing);
            attempt.preparing = null;
        }
        inflightRequests.values().forEach(wrapper -> {
            wrapper.outstandingTracked = false;
            processingQueue.addFirst(wrapper);
        });
        inflightRequests.clear();
        requestMetricsMap.clear();
        processingQueue.forEach(wrapper -> wrapper.outstandingTracked = false);
    }

    private boolean expireRecoveryIfOverdue(Map<String, AuditResponse> responses,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        if (recoveryEpisode == null || nanoTime.getAsLong() - recoveryDeadlineNanos < 0) return false;
        resultFuture.complete(new SastStreamOutcome.Failed(responses,
                new AviatorTechnicalException("Audit connection recovery timed out after 150 seconds")));
        return true;
    }

    private void endRecovery() {
        recoveryEpisode = null;
        if (recoveryTimeout != null) recoveryTimeout.cancel(false);
        recoveryTimeout = null;
    }

    private ClientResponseObserver<UserPromptRequest, AuditorResponse> createResponseObserver(
            Map<String, AuditResponse> responses, AtomicInteger processedRequests,
            CompletableFuture<SastStreamOutcome> resultFuture, Attempt attempt) {
        var streamLatch = attempt.done;
        var initLatch = attempt.initialized;
        var initializationTimeout = attempt.timeout;

        return new ClientResponseObserver<UserPromptRequest, AuditorResponse>() {
            private final AtomicBoolean isInitialized = new AtomicBoolean(false);

            @Override
            public void beforeStart(ClientCallStreamObserver<UserPromptRequest> requestStream) {
                synchronized (retryLock) {
                    if (isActive(attempt, resultFuture) && !expireRecoveryIfOverdue(responses, resultFuture)) {
                        attempt.handler.initialize(requestStream);
                    } else {
                        requestStream.cancel("Audit attempt closed", null);
                    }
                }
            }

            @Override
            public void onNext(AuditorResponse response) {
                synchronized (retryLock) {
                    if (isActive(attempt, resultFuture)) handleResponse(response);
                }
            }

            private void handleResponse(AuditorResponse response) {
                if (!isInitialized.get() && expireRecoveryIfOverdue(responses, resultFuture)) return;
                logger.info("Received response - Status: " + response.getStatus() + ", RequestId: " + response.getRequestId());

                if ("PONG".equals(response.getStatus())) {
                    logger.info("Received pong from server: StreamId: {}, Client timestamp: {}, Server timestamp: {}, RequestId: {}",
                            response.getStreamId(), response.getPong().getClientTimestamp(),
                            response.getPong().getServerTimestamp(), response.getRequestId());
                    return;
                }

                if (isInitialized.get() && "SERVER_BUSY".equals(response.getStatus())) {
                    handleServerBusy(response.getRequestId(), currentStreamState.actualIssuesCount,
                            processedRequests, responses, resultFuture, streamLatch);
                    return;
                }

                if ("INTERNAL_ERROR".equals(response.getStatus())) {
                    String cliMessage = "Internal server error occurred";
                    logger.error(cliMessage);
                    resultFuture.complete(new SastStreamOutcome.Failed(responses, new AviatorTechnicalException(cliMessage)));
                    streamLatch.countDown();
                    return;
                }

                if ("BACKPRESSURE_WARNING".equals(response.getStatus())) {
                    handleBackpressureWarning();
                } else if ("BACKPRESSURE_VIOLATION".equals(response.getStatus())) {
                    logger.error("Server terminated stream due to backpressure violations: {}", response.getStatusMessage());
                    if (!resultFuture.isDone()) {
                        resultFuture.complete(new SastStreamOutcome.Failed(responses,
                                new AviatorTechnicalException("Stream terminated by server: " + response.getStatusMessage())));
                    }
                    streamLatch.countDown();
                    return;
                } else {
                    consecutiveBackpressureViolations.set(0);
                    currentBackoff.set(1);
                }

                RequestWrapper completedWrapper = inflightRequests.remove(response.getRequestId());
                if (completedWrapper == null) {
                    if (!isInitialized.get()) {
                        if ("SUCCESS".equals(response.getStatus())) {
                            if (!initializationTimeout.initialized()) return;
                            if (response.hasQuota()) {
                                currentStreamState.quota = response.getQuota();
                                if (response.hasQuotaLastUpdated()) {
                                    currentStreamState.quotaLastUpdated = response.getQuotaLastUpdated();
                                    logger.info("Server quota received: {} (last updated: {})",
                                        currentStreamState.quota, currentStreamState.quotaLastUpdated);
                                } else {
                                    logger.info("Server quota received: {}", currentStreamState.quota);
                                }
                            } else {
                                LOG.debug("No quota limit received from server (unlimited or feature disabled)");
                            }
                            if (expireRecoveryIfOverdue(responses, resultFuture)) return;
                            isInitialized.set(true);
                            currentStreamState.isStreamInitialized = true;
                            endRecovery();
                            initLatch.countDown();
                            logger.info("Stream initialized successfully");
                            startPingPong(attempt);
                        } else {
                            if ("APPLICATION_BUSY".equals(response.getStatus())) {
                                failAttempt(attempt, new AviatorSimpleException("Application is busy"), true, responses, resultFuture);
                                return;
                            }
                            String errorMessage = "Stream initialization failed: " + response.getStatusMessage();
                            if (!resultFuture.isDone()) {
                                resultFuture.complete(new SastStreamOutcome.Failed(responses,
                                        new AviatorTechnicalException(errorMessage)));
                            }
                            streamLatch.countDown();
                        }
                    } else {
                        LOG.debug("Received response for an unknown or already processed requestId: {}", response.getRequestId());
                    }
                    return;
                }

                String instanceId = completedWrapper.userPrompt.getIssueData().getInstanceID();
                currentStreamState.processedIssueIds.add(instanceId);
                currentStreamState.pendingIssueIds.remove(instanceId);

                RequestMetrics metrics = requestMetricsMap.remove(response.getRequestId());
                if (metrics != null) {
                    metrics.complete(response.getStatus());
                    logger.info("Request {} ({}) completed with status {} in {}ms",
                            response.getRequestId(), instanceId, response.getStatus(), metrics.getDuration());
                }

                decrementOutstanding(completedWrapper);
                requestSemaphore.release();

                AuditResponse auditResponse = GrpcUtil.convertToAuditResponse(response);
                responses.put(instanceId, auditResponse);
                int completed = processedRequests.incrementAndGet();

                logger.progress("Processed " + completed + " out of " + currentStreamState.actualIssuesCount + " issues");

                if (completed >= currentStreamState.totalRequests) {
                    logger.info("All requests accounted for, completing stream.");
                    if (!resultFuture.isDone()) {
                        resultFuture.complete(new SastStreamOutcome.Completed(responses));
                    }
                    streamLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                synchronized (retryLock) {
                    failAttempt(attempt, t, isRetryableError(t), responses, resultFuture);
                }
            }

            @Override
            public void onCompleted() {
                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    if (expireRecoveryIfOverdue(responses, resultFuture)) return;
                    initializationTimeout.close();
                    initLatch.countDown();
                    stopPingPong();
                    logger.progress("Stream completed by server");
                    resultFuture.complete(new SastStreamOutcome.Completed(responses));
                    streamLatch.countDown();
                }
            }
        };
    }

    private void sendInitRequest(Attempt attempt, Map<String, AuditResponse> responses,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        String initRequestId = UUID.randomUUID().toString();
        StreamInitRequest.Builder initRequestBuilder = StreamInitRequest.newBuilder()
            .setStreamId(currentStreamState.streamId)
            .setRequestId(initRequestId)
            .setToken(currentStreamState.token)
            .setApplicationName(currentStreamState.projectName)
            .setSscApplicationName(currentStreamState.SSCApplicationName)
            .setSscApplicationVersion(currentStreamState.SSCApplicationVersion)
            .setTotalReportedIssues(currentStreamState.totalRequests)
            .setTotalIssuesToPredict(currentStreamState.totalRequests);

        if (!StringUtil.isEmpty(currentStreamState.FPRBuildId)) {
            initRequestBuilder.setFprBuildId(currentStreamState.FPRBuildId);
        }

        UserPromptRequest initRequest = UserPromptRequest.newBuilder()
            .setInit(initRequestBuilder.build())
            .build();

        observeSend(attempt.handler.sendRequest(initRequest), attempt, responses, resultFuture);
        LOG.info("Client Id for stream initialization {}", currentStreamState.streamId);
    }

    private void applyQuotaFilteringIfNeeded(int alreadyProcessed) {
        LOG.debug("Applying Quota based filter Quota for the application is {}", currentStreamState.quota);
        if (currentStreamState.quota <= 0) {
            LOG.debug("No quota filtering applied (quota not set or unlimited)");
            return;
        }

        int currentQueueSize = processingQueue.size();
        if (currentQueueSize <= currentStreamState.quota) {
            logger.progress("Queue size %d within quota %d, no filtering needed",
                currentQueueSize, currentStreamState.quota);
            return;
        }

        logger.progress("Queue size %d exceeds reserved quota %d. Applying priority-based filtering.",
            currentQueueSize, currentStreamState.quota);

        List<UserPrompt> allPrompts = new ArrayList<>();
        processingQueue.forEach(wrapper -> allPrompts.add(wrapper.userPrompt));

        Map<String, Integer> customPriorityMap = null;
        if (currentStreamState.customPriorityOrder != null &&
            !currentStreamState.customPriorityOrder.isEmpty()) {

            customPriorityMap = QuotaBasedFilter.buildCustomPriorityOrder(
                currentStreamState.customPriorityOrder);

            logger.info("Using custom folder priority order for filtering: {} (issues with priorities not in this list will be excluded)",
                       currentStreamState.customPriorityOrder);
        } else {
            logger.info("Using default priority order for filtering (Critical > High > Medium > Low)");
        }

        List<UserPrompt> filteredPrompts = QuotaBasedFilter.filterByQuota(
            allPrompts,
            currentStreamState.quota,
            customPriorityMap
        );

        processingQueue.clear();
        currentStreamState.pendingIssueIds.clear();

        filteredPrompts.stream()
            .map(RequestWrapper::new)
            .forEach(wrapper -> {
                processingQueue.add(wrapper);
                currentStreamState.pendingIssueIds.add(wrapper.userPrompt.getIssueData().getInstanceID());
            });

        currentStreamState.actualIssuesCount = alreadyProcessed + filteredPrompts.size();

        // Check if filtering resulted in zero issues
        if (filteredPrompts.isEmpty()) {
            String reason;
            if (customPriorityMap != null) {
                reason = String.format("0 issues to audit as per --folder-priority-order %s",
                    currentStreamState.customPriorityOrder);
            } else {
                reason = "0 issues to audit after quota-based filtering";
            }

            logger.progress("Quota filtering resulted in 0 issues to audit. Reason: %s", reason);
            logger.info("Aborting audit - all {} issues were filtered out", currentQueueSize);

            throw new AviatorQuotaFilterException(reason);
        }

        int excludedCount = currentQueueSize - filteredPrompts.size();
        if (filteredPrompts.size() < currentStreamState.quota && customPriorityMap != null) {
            logger.info("Filtering complete: {} issues retained out of {} (filtered out: {}, including issues with unknown priorities)",
                filteredPrompts.size(), currentQueueSize, excludedCount);
        } else {
            logger.info("Filtering complete: {} issues retained out of {} (filtered out: {})",
                filteredPrompts.size(), currentQueueSize, excludedCount);
        }
    }

    private boolean isRetryableError(Throwable t) {
        if (t instanceof StatusRuntimeException sre) {
            Status.Code code = sre.getStatus().getCode();
            String description = sre.getStatus().getDescription();
            if (code == Status.Code.INTERNAL && description != null &&
                    (description.contains("RST_STREAM") || description.contains("PROTOCOL_ERROR"))) {
                return true;
            }
            if (code == Status.Code.UNAVAILABLE) {
                return true;
            }
        }
        return false;
    }

    private long calculateStreamRetryDelay(int retryCount) {
        long delay = (long) (Constants.STREAM_RETRY_BASE_DELAY_MS * Math.pow(2, Math.min(retryCount - 1, 10)));
        delay = Math.min(delay, Constants.STREAM_RETRY_MAX_DELAY_MS);
        delay += ThreadLocalRandom.current().nextLong(1000);
        return delay;
    }

    private void processRequestQueue(int totalRequests, AtomicInteger processedRequests,
                                    Map<String, AuditResponse> responses,
                                    CompletableFuture<SastStreamOutcome> resultFuture,
                                    Attempt attempt) {
        var streamLatch = attempt.done;
        logger.progress("Starting to process issues...");

        LOG.info("Entering processRequestQueue loop, queue size: " + processingQueue.size() + ", permits: " + requestSemaphore.availablePermits() + ", outstanding: " + outstandingRequests.get());

        while (!client.isShutdown.get() && !attempt.handler.isCompleted() && !resultFuture.isDone()) {
            RequestWrapper wrapper = null;
            try {
                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    if (outstandingRequests.get() < 0) {
                        outstandingRequests.set(0);
                    }

                    if (processingQueue.isEmpty()) {
                        if (processedRequests.get() >= totalRequests || outstandingRequests.get() == 0) {
                            logger.info("Processing complete as queue is empty with no outstanding requests. Exiting loop.");
                            break;
                        }
                    } else if (requestSemaphore.tryAcquire()) {
                        wrapper = processingQueue.poll();
                        if (wrapper == null) requestSemaphore.release();
                        else attempt.preparing = wrapper;
                    }
                }
                if (wrapper == null) {
                    Thread.sleep(100);
                    continue;
                }

                SourceCodeEnricher.EnrichmentResult enrichmentResult = enrichSourceCode(wrapper);
                logger.info("Size of files {}", wrapper.userPrompt.getFiles().size());
                logger.info("Size of programming language {}", wrapper.userPrompt.getProgrammingLanguages().size());
                if (wrapper.attemptCount > 0) {
                    long delay = (long) (Constants.BASE_DELAY_MS * Math.pow(2, wrapper.attemptCount - 1));
                    delay = Math.min(delay, Constants.MAX_DELAY_MS) + ThreadLocalRandom.current().nextLong(100);
                    Thread.sleep(delay);
                }

                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    attempt.preparing = null;
                    String instanceId = wrapper.userPrompt.getIssueData().getInstanceID();

                    if (enrichmentResult != null && enrichmentResult.hasFailures()) {
                        completeSkippedRequest(wrapper, enrichmentResult, responses, processedRequests, totalRequests,
                                resultFuture, streamLatch);
                        continue;
                    }

                    if (currentStreamState.processedIssueIds.contains(instanceId)) {
                        requestSemaphore.release();
                        continue;
                    }

                    if (!wrapper.outstandingTracked) {
                        incrementOutstanding(wrapper);
                    }

                    LOG.info("Submitting request for instance " + instanceId);
                    submitUserPrompt(wrapper, attempt, responses, resultFuture);
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Thread interrupted while processing queue", ie));
                }
                break;
            } catch (FcliSimpleException e) {
                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    attempt.preparing = null;
                    if (wrapper != null) {
                        String instanceId = wrapper.userPrompt.getIssueData().getInstanceID();
                        LOG.error("Permanently failing request for issue {} due to a client-side error: {}", instanceId, e.getMessage());

                        AuditResponse failedResponse = new AuditResponse();
                        failedResponse.setIssueId(instanceId);
                        failedResponse.setStatus("FAILED");
                        failedResponse.setStatusMessage("Client-side pre-processing error: " + e.getMessage());
                        responses.put(instanceId, failedResponse);

                        currentStreamState.processedIssueIds.add(instanceId);
                        currentStreamState.pendingIssueIds.remove(instanceId);

                        int completed = processedRequests.incrementAndGet();
                        int failed = failedRequests.incrementAndGet();
                        decrementOutstanding(wrapper);
                        requestSemaphore.release();

                        logger.progress("Processed " + completed + " out of " + totalRequests + " issues (" + failed + " failed).");
                    } else {
                        LOG.error("Caught AviatorSimpleException but the request wrapper was null.", e);
                    }
                }
            } catch (Exception e) {
                synchronized (retryLock) {
                    if (!isActive(attempt, resultFuture)) return;
                    LOG.error("Unexpected error in processing loop, failing entire operation.", e);
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Unexpected error in processing loop", e));
                }
                break;
            }
        }

        synchronized (retryLock) {
            if (isActive(attempt, resultFuture)
                    && (processedRequests.get() >= totalRequests || (processingQueue.isEmpty() && outstandingRequests.get() == 0))) {
                logger.info("All requests have been accounted for. Completing operation and closing stream.");
                resultFuture.complete(new SastStreamOutcome.Completed(responses));
            }

            logger.info("Processing queue loop completed. Queue size: {}, Processed: {}/{}, Outstanding: {}",
                    processingQueue.size(), processedRequests.get(), totalRequests, outstandingRequests.get());
        }
    }

    private SourceCodeEnricher.EnrichmentResult enrichSourceCode(RequestWrapper wrapper) {
        synchronized (wrapper) {
            if (wrapper.sourceCodeEnriched) {
                return null;
            }

            SourceCodeEnricher.EnrichmentResult enrichmentResult =
                    sourceCodeEnricher.enrichWithSourceCodeDetailed(wrapper.userPrompt.getStackTrace());
            if (!enrichmentResult.hasFailures()) {
                List<File> sourceCodeFiles =
                        new ArrayList<>(enrichmentResult.files().values());
                wrapper.userPrompt.getFiles().addAll(sourceCodeFiles);
                wrapper.sourceCodeEnriched = true;
            }
            return enrichmentResult;
        }
    }

    private void incrementOutstanding(RequestWrapper wrapper) {
        if (!wrapper.outstandingTracked) {
            outstandingRequests.incrementAndGet();
            wrapper.outstandingTracked = true;
        }
    }

    private void decrementOutstanding(RequestWrapper wrapper) {
        if (wrapper != null && wrapper.outstandingTracked) {
            wrapper.outstandingTracked = false;
            if (outstandingRequests.decrementAndGet() < 0) {
                outstandingRequests.set(0);
            }
        }
    }

    private void completeSkippedRequest(RequestWrapper wrapper, SourceCodeEnricher.EnrichmentResult enrichmentResult,
                                        Map<String, AuditResponse> responses, AtomicInteger processedRequests,
                                        int totalRequests, CompletableFuture<SastStreamOutcome> resultFuture,
                                        CountDownLatch streamLatch) {
        String instanceId = wrapper.userPrompt.getIssueData().getInstanceID();
        AuditSkipReason skipReason = enrichmentResult.failures().get(0).reason();
        String failureMessage = formatSourceFailureMessage(enrichmentResult, skipReason);
        AuditResponse skippedResponse = new AuditResponse();
        skippedResponse.setIssueId(instanceId);
        skippedResponse.setStatus("SKIPPED");
        skippedResponse.setAuditSkipReason(skipReason);
        skippedResponse.setStatusMessage(failureMessage);
        responses.put(instanceId, skippedResponse);

        currentStreamState.processedIssueIds.add(instanceId);
        currentStreamState.pendingIssueIds.remove(instanceId);
        decrementOutstanding(wrapper);
        requestSemaphore.release();

        int completed = processedRequests.incrementAndGet();
        int skipped = skippedRequests.incrementAndGet();
        LOG.warn("Skipping issue {}: {}", instanceId, failureMessage);
        logger.progress("Processed " + completed + " out of " + totalRequests + " issues (" + skipped + " skipped).");

        if (completed >= totalRequests) {
            logger.info("All requests accounted for, completing stream.");
            streamLatch.countDown();
            if (!resultFuture.isDone()) {
                resultFuture.complete(new SastStreamOutcome.Completed(responses));
            }
        }
    }

    private String formatSourceFailureMessage(SourceCodeEnricher.EnrichmentResult enrichmentResult,
                                              AuditSkipReason skipReason) {
        List<String> filenames = enrichmentResult.failures().stream()
                .map(SourceCodeEnricher.SourceFileFailure::filename)
                .distinct()
                .collect(Collectors.toList());
        String details = enrichmentResult.failures().stream()
                .map(SourceCodeEnricher.SourceFileFailure::message)
                .filter(message -> message != null && !message.isBlank())
                .distinct()
                .collect(Collectors.joining("; "));
        String detailSuffix = details.isBlank() ? "" : " (" + details + ")";
        return switch (skipReason) {
            case SOURCE_FILE_DECODE_FAILED -> skipReason.format(
                    filenames.size() == 1 ? "" : "s", String.join(", ", filenames), detailSuffix);
            case SOURCE_FILE_READ_FAILED -> skipReason.format(String.join(", ", filenames), detailSuffix);
            case SKIPPED_BY_AVIATOR -> details.isBlank() ? skipReason.getDisplayMessage() : details;
        };
    }

    private void handleServerBusy(String requestId, int totalRequests, AtomicInteger processedRequests,
            Map<String, AuditResponse> responses, CompletableFuture<SastStreamOutcome> resultFuture, CountDownLatch streamLatch) {
        RequestWrapper wrapperToRetry = inflightRequests.remove(requestId);
        if (wrapperToRetry == null) {
            LOG.warn("WARN: Received SERVER_BUSY for unknown or already completed requestId: {}", requestId);
            return;
        }

        requestMetricsMap.remove(requestId);
        requestSemaphore.release();

        wrapperToRetry.attemptCount++;

        if (wrapperToRetry.attemptCount > Constants.MAX_RETRIES) {
            LOG.error("Request for instance {} failed after {} retries due to server being busy. Dropping request.", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), Constants.MAX_RETRIES);

            AuditResponse failedResponse = new AuditResponse();
            failedResponse.setIssueId(wrapperToRetry.userPrompt.getIssueData().getInstanceID());
            failedResponse.setStatus("RETRY_LIMIT_EXCEEDED");
            failedResponse.setStatusMessage("Request failed after " + Constants.MAX_RETRIES + " retries due to server overload.");
            failedResponse.setSubmittedToAviator(true);
            responses.put(wrapperToRetry.userPrompt.getIssueData().getInstanceID(), failedResponse);
            int completed = processedRequests.incrementAndGet();

            logger.progress("Request permanently failed due to server busy - Processed " + completed + " out of " + totalRequests + " issues");
            decrementOutstanding(wrapperToRetry);
            int stillOutstanding = outstandingRequests.get();
            LOG.warn("WARN: Request for instance {} permanently failed. Remaining outstanding requests: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), stillOutstanding);
            if (completed >= totalRequests) {
                logger.info("All requests accounted for after permanent failure, completing stream.");
                if (!resultFuture.isDone()) resultFuture.complete(new SastStreamOutcome.Completed(responses));
                streamLatch.countDown();
            }
        } else {
            LOG.warn("WARN: Server is busy for instanceId {}. Re-queueing for retry (Attempt {}/{}). Queue size before re-queue: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), wrapperToRetry.attemptCount, Constants.MAX_RETRIES, processingQueue.size());

            processingQueue.addFirst(wrapperToRetry);

            logger.info("Request re-queued. Queue size after re-queue: {}", processingQueue.size());
        }
    }

    private void submitUserPrompt(RequestWrapper wrapper, Attempt attempt, Map<String, AuditResponse> responses,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        String requestId = UUID.randomUUID().toString();
        inflightRequests.put(requestId, wrapper);

        AuditRequest auditRequest = GrpcUtil.convertToAuditRequest(wrapper.userPrompt, currentStreamState.streamId, requestId);
        UserPromptRequest promptRequest = UserPromptRequest.newBuilder().setAudit(auditRequest).build();

        requestMetricsMap.put(requestId, new RequestMetrics());

        if (promptRequest.getSerializedSize() > Constants.MAX_MESSAGE_SIZE) {
            throw new AviatorSimpleException("Message size exceeds maximum allowed limit");
        }
        observeSend(attempt.handler.sendRequest(promptRequest), attempt, responses, resultFuture);
    }

    private void observeSend(CompletableFuture<Boolean> send, Attempt attempt, Map<String, AuditResponse> responses,
            CompletableFuture<SastStreamOutcome> resultFuture) {
        send.whenComplete((sent, error) -> {
            synchronized (retryLock) {
                if (error != null || !Boolean.TRUE.equals(sent)) {
                    Throwable failure = error != null ? error : attempt.handler.getSendFailure().getNow(null);
                    if (failure == null) {
                        failure = Status.UNAVAILABLE.withDescription("Audit request could not be sent").asRuntimeException();
                    }
                    failAttempt(attempt, failure, isRetryableError(failure), responses, resultFuture);
                }
            }
        });
    }

    private void handleBackpressureWarning() {
        long now = System.currentTimeMillis();
        long last = lastBackpressureViolation.getAndSet(now);

        if (now - last < 5000) {
            int violations = consecutiveBackpressureViolations.incrementAndGet();
            if (violations > 1) {
                int newBackoff = Math.min(10, currentBackoff.get() * 2);
                currentBackoff.set(newBackoff);

                int currentWindow = serverWindowSize.get();
                int newWindow = Math.max(20, currentWindow / 2);
                serverWindowSize.set(newWindow);

                LOG.warn("WARN: Received multiple backpressure warnings. Reducing window to {} and setting backoff to {}x", newWindow, newBackoff);
            }
        } else {
            consecutiveBackpressureViolations.set(1);
        }
    }

    private void startPingPong(Attempt attempt) {
        if (isPinging.compareAndSet(false, true)) {
            logger.info("Starting ping-pong keepalive with interval of {} seconds", pingIntervalSeconds);
            pingTask = pingScheduler.scheduleAtFixedRate(() -> sendPing(attempt),
                    pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS);
        }
    }

    private void stopPingPong() {
        if (isPinging.compareAndSet(true, false) && pingTask != null) {
            logger.info("Stopping ping-pong keepalive");
            pingTask.cancel(false);
            pingTask = null;
        }
    }

    private void sendPing(Attempt attempt) {
        synchronized (retryLock) {
            if (activeAttempt != attempt || attempt.finished || closed) return;
            try {
                if (requestHandler != null && requestHandler.isReady() && currentStreamState != null) {
                    PingRequest pingRequest = PingRequest.newBuilder()
                            .setStreamId(currentStreamState.streamId)
                            .setTimestamp(System.currentTimeMillis())
                            .build();

                    UserPromptRequest pingMsg = UserPromptRequest.newBuilder().setPing(pingRequest).build();
                    requestHandler.getRequestQueue().addFirst(pingMsg);
                    var handler = requestHandler;
                    CompletableFuture.supplyAsync(handler::flush);
                    LOG.info("ping  streamId: {}", currentStreamState.streamId);
                }
            } catch (Exception e) {
                if (requestHandler != null && !requestHandler.isCompleted()) {
                    LOG.warn("WARN: Failed to send ping: {}", e.getMessage());
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (retryLock) {
            closed = true;
            endRecovery();
            if (retryDelay != null) retryDelay.cancel(false);
            if (retryDelayLatch != null) retryDelayLatch.countDown();
            if (activeAttempt != null) retireAttempt(activeAttempt, false);
            if (activeResult != null && !activeResult.isDone()) {
                activeResult.completeExceptionally(new AviatorTechnicalException("Audit stream closed"));
            }
        }
    }
}
