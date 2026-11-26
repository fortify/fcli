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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.dastaudit.DastAuditClientMessage;
import com.fortify.aviator.dastaudit.DastAuditErrorResponse;
import com.fortify.aviator.dastaudit.DastAuditInitResponse;
import com.fortify.aviator.dastaudit.DastAuditPingRequest;
import com.fortify.aviator.dastaudit.DastAuditPongResponse;
import com.fortify.aviator.dastaudit.DastAuditRequest;
import com.fortify.aviator.dastaudit.DastAuditResponse;
import com.fortify.aviator.dastaudit.DastAuditServerMessage;
import com.fortify.aviator.dastaudit.DastAuditServiceGrpc;
import com.fortify.aviator.dastaudit.DastAuditStreamInitRequest;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;

import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

/**
 * Processes DAST findings through the Aviator DAST audit bidirectional stream.
 */
public class DastAuditStreamProcessor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DastAuditStreamProcessor.class);

    private final IAviatorLogger logger;
    private final DastAuditServiceGrpc.DastAuditServiceStub asyncStub;
    private final ScheduledExecutorService pingScheduler;
    private final long pingIntervalSeconds;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicBoolean isRpcCompleted = new AtomicBoolean(false);
    private final AtomicBoolean isRetryScheduled = new AtomicBoolean(false);
    private final AtomicLong streamGeneration = new AtomicLong();
    private boolean streamInitialized;

    private RequestHandler<DastAuditClientMessage> requestHandler;
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> retryTask;
    private ClientCallStreamObserver<DastAuditClientMessage> activeRequestStream;
    private String streamId;
    private DastAuditStreamConfig config;
    private List<DastAuditWorkItem> workItems;
    private int totalReportedIssues;
    private CompletableFuture<DastAuditStreamResult> resultFuture;
    private final List<DastAuditResult> results = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> requestIssueIds = new ConcurrentHashMap<>();
    private final Map<String, String> requestIdsByIssue = new ConcurrentHashMap<>();
    private final Set<String> completedRequestIds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingIssueIds = ConcurrentHashMap.newKeySet();
    private final RequestSendWindow<DastAuditClientMessage> sendWindow = new RequestSendWindow<>(
        Constants.INITIAL_REQUEST_WINDOW, message -> message.getAudit().getRequestId());
    private final Map<String, DastAuditClientMessage> requestMessages = new ConcurrentHashMap<>();
    private final Map<String, Integer> serverBusyAttempts = new ConcurrentHashMap<>();
    private volatile StreamInitializationTimeout initializationTimeout;
    private int reservedQuota;
    private int exceededCount;
    private boolean unlimitedQuota;
    private String quotaLastUpdated;
    private String nextQuotaUpdateMessage;
    private final StreamConnectionRecovery recovery;

    public DastAuditStreamProcessor(
            IAviatorLogger logger,
            DastAuditServiceGrpc.DastAuditServiceStub asyncStub,
            ScheduledExecutorService pingScheduler,
            long pingIntervalSeconds) {
        this(logger, asyncStub, pingScheduler, pingIntervalSeconds, System::nanoTime);
    }

    DastAuditStreamProcessor(IAviatorLogger logger, DastAuditServiceGrpc.DastAuditServiceStub asyncStub,
            ScheduledExecutorService pingScheduler, long pingIntervalSeconds, LongSupplier nanoTime) {
        this.logger = logger;
        this.asyncStub = asyncStub;
        this.pingScheduler = pingScheduler;
        this.pingIntervalSeconds = pingIntervalSeconds;
        this.recovery = new StreamConnectionRecovery(pingScheduler, this, nanoTime, () ->
            completeExceptionally(new AviatorTechnicalException("DAST audit connection recovery timed out")));
    }

    public synchronized CompletableFuture<DastAuditStreamResult> process(
            DastAuditStreamConfig config,
            List<DastAuditWorkItem> workItems,
            int totalReportedIssues) {
        this.config = config;
        this.totalReportedIssues = totalReportedIssues;
        this.resultFuture = new CompletableFuture<>();
        this.requestIdsByIssue.clear();
        this.completedRequestIds.clear();
        this.results.clear();
        this.isClosed.set(false);
        initializeWorkItems(workItems);
        resultFuture.whenComplete((result, error) -> {
            synchronized (this) {
                recovery.end();
                closeInitializationTimeout();
                stopPingPong();
                streamGeneration.incrementAndGet();
                if (retryTask != null) retryTask.cancel(false);
                if (error != null && !isRpcCompleted.get() && activeRequestStream != null) {
                    activeRequestStream.cancel("DAST audit run ended", error);
                }
                if (requestHandler != null && error != null) requestHandler.abort(error);
            }
        });
        AviatorGrpcClient.registerStream(pingScheduler, this::close, resultFuture);
        startStream();
        return resultFuture;
    }

    void initializeWorkItems(List<DastAuditWorkItem> items) {
        this.workItems = List.copyOf(items);
        this.pendingIssueIds.clear();
        items.forEach(item -> pendingIssueIds.add(item.issue().getId()));
    }

    private void startStream() {
        if (resultFuture.isDone() || isClosed.get() || recovery.expireIfOverdue()) return;
        this.streamId = UUID.randomUUID().toString();
        this.requestIssueIds.clear();
        this.sendWindow.clear();
        this.requestMessages.clear();
        this.serverBusyAttempts.clear();
        this.requestHandler = new RequestHandler<>(streamId);
        this.activeRequestStream = null;
        this.isRpcCompleted.set(false);
        this.isRetryScheduled.set(false);
        this.streamInitialized = false;
        long generation = streamGeneration.incrementAndGet();
        requestHandler.getSendFailure().thenAcceptAsync(error -> {
            synchronized (this) {
                if (generation == streamGeneration.get() && !resultFuture.isDone()) handleStreamError(error);
            }
        });
        LOG.debug("Starting DAST audit stream {} with {} pending findings and {} reported findings",
            streamId, pendingIssueIds.size(), totalReportedIssues);

        try {
            var attemptTimeout = new StreamInitializationTimeout(pingScheduler,
                () -> {
                    synchronized (this) {
                        if (generation != streamGeneration.get() || resultFuture.isDone()) return;
                        handleStreamError(StreamInitializationTimeout.expiredError("DAST audit"), recovery.active());
                    }
                });
            initializationTimeout = attemptTimeout;
            StreamObserver<DastAuditClientMessage> requestObserver =
                asyncStub.processDastAuditStream(new ResponseObserver(generation));
            if (generation != streamGeneration.get() || resultFuture.isDone()) return;
            requestHandler.initialize(requestObserver);
            startPingPong();
            sendInit();
            attemptTimeout.arm();
        } catch (RuntimeException exception) {
            handleStreamError(exception);
        }
    }

    private void sendInit() {
        var init = DastAuditStreamInitRequest.newBuilder()
            .setToken(value(config.token()))
            .setApplicationName(value(config.applicationName()))
            .setStreamId(streamId)
            .setRequestId(UUID.randomUUID().toString())
            .setTotalReportedIssues(totalReportedIssues)
            .setTotalIssuesToAudit(pendingIssueIds.size());
        if (config.fprBuildId() != null) init.setFprBuildId(config.fprBuildId());
        if (config.sscApplicationName() != null) init.setSscApplicationName(config.sscApplicationName());
        if (config.sscApplicationVersion() != null) init.setSscApplicationVersion(config.sscApplicationVersion());
        sendRequest(DastAuditClientMessage.newBuilder().setInit(init).build());
    }

    private void handleInit(DastAuditInitResponse response) {
        if (streamInitialized || recovery.expireIfOverdue()) return;
        if ("APPLICATION_BUSY".equals(response.getStatus())) {
            handleStreamError(new AviatorSimpleException("Application is busy"), true);
            return;
        }
        if (!initializationTimeout.initialized()) return;
        if (!isSuccess(response.getStatus())) {
            fail(new AviatorSimpleException("DAST audit initialization failed: " + response.getStatusMessage()));
            return;
        }
        streamInitialized = true;
        recovery.end();
        reservedQuota = response.getReservedQuota();
        exceededCount = response.getExceededCount();
        unlimitedQuota = response.getUnlimitedQuota();
        quotaLastUpdated = response.hasQuotaLastUpdated() ? response.getQuotaLastUpdated() : null;
        nextQuotaUpdateMessage = response.hasNextQuotaUpdateMessage() ? response.getNextQuotaUpdateMessage() : null;
        List<DastAuditWorkItem> remainingWorkItems = remainingWorkItems();
        logger.info("DAST audit stream initialized; submitting " + remainingWorkItems.size() + " findings");
        sendWindow.enqueue(prepareAuditRequests(remainingWorkItems, streamId));
        sendQueuedRequests();
    }

    /** Sends queued audit requests while the in-flight window has free slots. */
    private void sendQueuedRequests() {
        if (!streamInitialized || isRetryScheduled.get() || requestHandler == null
                || requestHandler.isCompleted() || resultFuture.isDone()) return;
        for (DastAuditClientMessage request : sendWindow.drainSendable()) {
            sendRequest(request);
        }
    }

    List<DastAuditClientMessage> prepareAuditRequests(List<DastAuditWorkItem> items, String requestStreamId) {
        List<DastAuditClientMessage> requests = new ArrayList<>(items.size());
        for (DastAuditWorkItem item : items) {
            String issueId = item.issue().getId();
            String requestId = requestIdsByIssue.computeIfAbsent(issueId, ignored -> UUID.randomUUID().toString());
            var request = DastAuditRequest.newBuilder()
                .setRequestId(requestId)
                .setStreamId(requestStreamId)
                .setFinding(DastAuditRequestMapper.toFindingContext(item.session(), item.issue()))
                .build();
            pendingIssueIds.add(issueId);
            requestIssueIds.put(requestId, issueId);
            LOG.debug("Preparing DAST issue {} with request {} on stream {}",
                issueId, requestId, requestStreamId);
            var message = DastAuditClientMessage.newBuilder().setAudit(request).build();
            requestMessages.put(requestId, message);
            requests.add(message);
        }
        return List.copyOf(requests);
    }

    int pendingRequestCount() {
        return pendingIssueIds.size();
    }

    List<DastAuditWorkItem> remainingWorkItems() {
        return workItems.stream()
            .filter(item -> pendingIssueIds.contains(item.issue().getId()))
            .toList();
    }

    String completeRequest(String requestId) {
        String issueId = requestIssueIds.remove(requestId);
        if (issueId != null) {
            pendingIssueIds.remove(issueId);
            completedRequestIds.add(requestId);
            requestMessages.remove(requestId);
            serverBusyAttempts.remove(requestId);
        }
        sendWindow.release(requestId);
        return issueId;
    }

    private void handleAudit(DastAuditResponse response) {
        String issueId = completeRequest(response.getRequestId());
        if (issueId == null) {
            LOG.warn("Ignoring DAST audit response for unknown or completed request {}", response.getRequestId());
            return;
        }
        var decision = response.hasDecision() ? response.getDecision() : null;
        LOG.debug("Received DAST audit response: issueId={}, requestId={}, status={}, confidence={}, tier={}, hasDecision={}",
            issueId, response.getRequestId(), response.getStatus(),
            decision != null ? decision.getConfidence() : null,
            decision != null ? decision.getTier() : null, decision != null);
        results.add(DastAuditResponseMapper.map(response, issueId));
        logger.progress("Audited %d of %d DAST findings", results.size(), workItems.size());
        completeRequestsIfDone();
        sendQueuedRequests();
    }

    private void handleError(DastAuditErrorResponse response) {
        if (!streamInitialized && ("APPLICATION_BUSY".equals(response.getStatus())
                || recovery.active() && ServerBusyBackoff.isServerBusy(response.getStatus()))) {
            handleStreamError(new AviatorSimpleException("Aviator server is busy"), true);
            return;
        }
        if (ServerBusyBackoff.isServerBusy(response.getStatus()) && scheduleServerBusyRetry(response.getRequestId())) {
            return;
        }
        String issueId = completeRequest(response.getRequestId());
        if (issueId != null) {
            LOG.debug("Received DAST audit error response: issueId={}, requestId={}, status={}, statusMessage={}",
                issueId, response.getRequestId(), response.getStatus(), response.getStatusMessage());
            DastAuditResult result = "SKIPPED".equalsIgnoreCase(response.getStatus())
                ? DastAuditResult.Skipped.builder()
                    .issueId(issueId)
                    .statusMessage(response.getStatusMessage())
                    .build()
                : DastAuditResult.Failure.builder()
                    .issueId(issueId)
                    .status(response.getStatus())
                    .statusMessage(response.getStatusMessage())
                    .build();
            results.add(result);
            logger.progress("Audited %d of %d DAST findings", results.size(), workItems.size());
            completeRequestsIfDone();
            sendQueuedRequests();
        } else {
            if (completedRequestIds.contains(response.getRequestId())) {
                LOG.debug("Ignoring duplicate DAST audit error for completed request {}", response.getRequestId());
                return;
            }
            fail(new AviatorSimpleException("DAST audit error: " + response.getStatusMessage()));
        }
    }

    /**
     * Re-sends a request the server rejected as overloaded, after a backoff. The request keeps its
     * window slot while waiting, so retries cannot push in-flight work above the window.
     * Returns false when the request is unknown or has exhausted its retries.
     */
    private boolean scheduleServerBusyRetry(String requestId) {
        DastAuditClientMessage message = requestMessages.get(requestId);
        if (message == null || !requestIssueIds.containsKey(requestId)) return false;

        int attempt = serverBusyAttempts.merge(requestId, 1, Integer::sum);
        if (attempt > Constants.MAX_RETRIES) {
            LOG.warn("DAST audit request {} rejected as server busy {} times; giving up", requestId, attempt - 1);
            return false;
        }

        long delay = ServerBusyBackoff.delayMs(attempt);
        LOG.warn("Server busy for DAST audit request {}; retrying in {} ms (attempt {}/{})",
            requestId, delay, attempt, Constants.MAX_RETRIES);

        long generation = streamGeneration.get();
        Runnable resend = () -> {
            synchronized (this) {
                if (generation != streamGeneration.get() || resultFuture.isDone() || isClosed.get()
                        || !requestIssueIds.containsKey(requestId)) return;
                sendRequest(message);
            }
        };
        if (pingScheduler == null) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(resend);
        } else {
            pingScheduler.schedule(resend, delay, TimeUnit.MILLISECONDS);
        }
        return true;
    }

    private void handlePong(DastAuditPongResponse response) {
        LOG.debug("DAST audit pong received in {} ms", System.currentTimeMillis() - response.getClientTimestamp());
    }

    private void completeRequestsIfDone() {
        if (pendingIssueIds.isEmpty() && requestHandler != null && !requestHandler.isCompleted()) {
            requestHandler.complete();
        }
    }

    private class ResponseObserver implements ClientResponseObserver<DastAuditClientMessage, DastAuditServerMessage> {
        private final long generation;

        private ResponseObserver(long generation) {
            this.generation = generation;
        }

        private boolean isCurrent() {
            return generation == streamGeneration.get();
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<DastAuditClientMessage> requestStream) {
            synchronized (DastAuditStreamProcessor.this) {
                if (isCurrent() && !resultFuture.isDone()) activeRequestStream = requestStream;
                else requestStream.cancel("DAST audit attempt retired", null);
            }
        }

        @Override
        public void onNext(DastAuditServerMessage message) {
            synchronized (DastAuditStreamProcessor.this) {
                if (!isCurrent() || resultFuture.isDone() || recovery.expireIfOverdue()) return;
                try {
                    switch (message.getResponseTypeCase()) {
                        case INIT -> handleInit(message.getInit());
                        case AUDIT -> handleAudit(message.getAudit());
                        case ERROR -> handleError(message.getError());
                        case PONG -> handlePong(message.getPong());
                        default -> LOG.warn("Unknown DAST audit response type: {}", message.getResponseTypeCase());
                    }
                } catch (RuntimeException error) {
                    completeExceptionally(new AviatorTechnicalException("Error handling DAST audit response", error));
                }
            }
        }

        @Override
        public void onError(Throwable throwable) {
            synchronized (DastAuditStreamProcessor.this) {
                if (!isCurrent()) return;
                isRpcCompleted.set(true);
                handleStreamError(throwable);
            }
        }

        @Override
        public void onCompleted() {
            synchronized (DastAuditStreamProcessor.this) {
                if (!isCurrent()) return;
                isRpcCompleted.set(true);
                stopPingPong();
                if (resultFuture.isDone()) return;
                if (!streamInitialized || !pendingIssueIds.isEmpty()) {
                    handleStreamError(Status.UNAVAILABLE
                        .withDescription("DAST audit stream completed before all findings received terminal responses")
                        .asRuntimeException());
                    return;
                }
                completeSuccessfully();
            }
        }
    }

    private void startPingPong() {
        if (pingScheduler == null || pingIntervalSeconds <= 0) return;
        long generation = streamGeneration.get();
        pingTask = pingScheduler.scheduleWithFixedDelay(() -> {
            synchronized (this) {
                if (generation != streamGeneration.get() || resultFuture.isDone() || isClosed.get()) return;
                if (requestHandler != null && requestHandler.isReady()) {
                    var ping = DastAuditPingRequest.newBuilder()
                        .setStreamId(streamId)
                        .setTimestamp(System.currentTimeMillis())
                        .build();
                    sendRequest(DastAuditClientMessage.newBuilder().setPing(ping).build());
                }
            }
        }, pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS);
    }

    private void handleStreamError(Throwable throwable) {
        handleStreamError(throwable, isRetryableError(throwable));
    }

    private void handleStreamError(Throwable throwable, boolean retryable) {
        closeInitializationTimeout();
        stopPingPong();
        if (resultFuture.isDone() || isClosed.get() || isRetryScheduled.get()) return;

        if (recovery.expireIfOverdue()) return;
        if (retryable && streamInitialized && pendingIssueIds.isEmpty()) {
            logger.warn("WARN: DAST audit stream disconnected after all finding responses were received");
            completeSuccessfully();
            return;
        }
        if (retryable) {
            if (!isRetryScheduled.compareAndSet(false, true)) return;
            recovery.begin();
            long delay = recovery.nextDelayMs();
            logger.warn("WARN: Retrying DAST audit stream connection after %d ms delay; %d findings remain",
                delay, pendingIssueIds.size());
            scheduleRetry(delay, throwable);
            return;
        }

        Status status = Status.fromThrowable(throwable);
        completeExceptionally(throwable instanceof AviatorSimpleException simple ? simple
            : new AviatorTechnicalException("DAST audit stream failed: " + status.getDescription(), throwable));
    }

    private void scheduleRetry(long delay, Throwable error) {
        long generation = streamGeneration.incrementAndGet();
        streamInitialized = false;
        if (requestHandler != null) requestHandler.abort(error);
        var run = resultFuture;
        Runnable retry = () -> {
            synchronized (this) {
                if (run == resultFuture && generation == streamGeneration.get() && !run.isDone() && !isClosed.get()) {
                    isRetryScheduled.set(false);
                    startStream();
                }
            }
        };
        if (!isRpcCompleted.get() && activeRequestStream != null) {
            activeRequestStream.cancel("Retrying DAST audit stream", null);
        }
        if (pingScheduler == null) {
            CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS).execute(retry);
        } else {
            retryTask = pingScheduler.schedule(retry, delay, TimeUnit.MILLISECONDS);
        }
    }

    static boolean isRetryableError(Throwable throwable) {
        return StreamConnectionRecovery.isRetryable(throwable);
    }

    private void sendRequest(DastAuditClientMessage request) {
        RequestHandler<DastAuditClientMessage> currentHandler = requestHandler;
        long generation = streamGeneration.get();
        currentHandler.sendRequest(request).whenComplete((sent, throwable) -> {
            synchronized (this) {
                if (generation != streamGeneration.get() || currentHandler != requestHandler
                        || resultFuture.isDone() || isClosed.get()) return;
                if (throwable != null || !Boolean.TRUE.equals(sent)) {
                    Throwable cause = throwable != null ? throwable : currentHandler.getSendFailure().getNow(null);
                    handleStreamError(cause != null ? cause : Status.UNAVAILABLE
                        .withDescription("Unable to send DAST audit stream request").asRuntimeException());
                }
            }
        });
    }

    private void fail(RuntimeException exception) {
        stopPingPong();
        completeExceptionally(exception);
    }

    private void completeExceptionally(RuntimeException exception) {
        if (!resultFuture.isDone()) resultFuture.completeExceptionally(exception);
    }

    private void completeSuccessfully() {
        if (!resultFuture.isDone()) {
            resultFuture.complete(DastAuditStreamResult.builder()
                .results(List.copyOf(results))
                .reservedQuota(reservedQuota)
                .exceededCount(exceededCount)
                .unlimitedQuota(unlimitedQuota)
                .quotaLastUpdated(quotaLastUpdated)
                .nextQuotaUpdateMessage(nextQuotaUpdateMessage)
                .build());
        }
    }

    private void stopPingPong() {
        if (pingTask != null) pingTask.cancel(false);
    }

    private void closeInitializationTimeout() {
        var timeout = initializationTimeout;
        if (timeout != null) timeout.close();
    }

    private boolean isSuccess(String status) {
        return "SUCCESS".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(status);
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    @Override
    public synchronized void close() {
        isClosed.set(true);
        if (resultFuture != null && !resultFuture.isDone()) {
            completeExceptionally(new AviatorTechnicalException("DAST audit stream processor closed"));
        }
    }
}