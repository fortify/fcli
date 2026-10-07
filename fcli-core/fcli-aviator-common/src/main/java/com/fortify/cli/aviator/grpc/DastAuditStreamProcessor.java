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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
    private final AtomicBoolean isPinging = new AtomicBoolean(false);
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final AtomicBoolean isRpcCompleted = new AtomicBoolean(false);
    private final AtomicBoolean isRetryScheduled = new AtomicBoolean(false);
    private final AtomicInteger streamRetryCount = new AtomicInteger();
    private final AtomicInteger stagnantRetryCount = new AtomicInteger();
    private final AtomicLong streamGeneration = new AtomicLong();

    private RequestHandler<DastAuditClientMessage> requestHandler;
    private ScheduledFuture<?> pingTask;
    private ScheduledFuture<?> retryTask;
    private ClientCallStreamObserver<DastAuditClientMessage> activeRequestStream;
    private String streamId;
    private DastAuditStreamConfig config;
    private List<DastAuditWorkItem> workItems;
    private int totalReportedIssues;
    private int lastRetryCompletedCount;
    private CompletableFuture<DastAuditStreamResult> resultFuture;
    private final List<DastAuditResult> results = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> requestIssueIds = new ConcurrentHashMap<>();
    private final Map<String, String> requestIdsByIssue = new ConcurrentHashMap<>();
    private final Set<String> completedRequestIds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingIssueIds = ConcurrentHashMap.newKeySet();
    private int reservedQuota;
    private int exceededCount;
    private boolean unlimitedQuota;
    private boolean quotaMetadataInitialized;
    private String quotaLastUpdated;
    private String nextQuotaUpdateMessage;

    public DastAuditStreamProcessor(
            IAviatorLogger logger,
            DastAuditServiceGrpc.DastAuditServiceStub asyncStub,
            ScheduledExecutorService pingScheduler,
            long pingIntervalSeconds) {
        this.logger = logger;
        this.asyncStub = asyncStub;
        this.pingScheduler = pingScheduler;
        this.pingIntervalSeconds = pingIntervalSeconds;
    }

    public CompletableFuture<DastAuditStreamResult> process(
            DastAuditStreamConfig config,
            List<DastAuditWorkItem> workItems,
            int totalReportedIssues) {
        this.config = config;
        this.totalReportedIssues = totalReportedIssues;
        this.resultFuture = new CompletableFuture<>();
        this.requestIdsByIssue.clear();
        this.completedRequestIds.clear();
        this.results.clear();
        this.streamRetryCount.set(0);
        this.stagnantRetryCount.set(0);
        this.lastRetryCompletedCount = 0;
        this.isClosed.set(false);
        this.quotaMetadataInitialized = false;
        initializeWorkItems(workItems);
        startStream();
        return resultFuture;
    }

    void initializeWorkItems(List<DastAuditWorkItem> items) {
        this.workItems = List.copyOf(items);
        this.pendingIssueIds.clear();
        items.forEach(item -> pendingIssueIds.add(item.issue().getId()));
    }

    private void startStream() {
        if (resultFuture.isDone() || isClosed.get()) return;
        this.streamId = UUID.randomUUID().toString();
        this.requestIssueIds.clear();
        this.requestHandler = new RequestHandler<>(streamId);
        this.activeRequestStream = null;
        this.isRpcCompleted.set(false);
        this.isRetryScheduled.set(false);
        long generation = streamGeneration.incrementAndGet();
        LOG.debug("Starting DAST audit stream {} with {} pending findings and {} reported findings",
            streamId, pendingIssueIds.size(), totalReportedIssues);

        try {
            StreamObserver<DastAuditClientMessage> requestObserver =
                asyncStub.processDastAuditStream(new ResponseObserver(generation));
            requestHandler.initialize(requestObserver);
            startPingPong();
            sendInit();
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
        if (!isSuccess(response.getStatus())) {
            fail(new AviatorSimpleException("DAST audit initialization failed: " + response.getStatusMessage()));
            return;
        }
        if (!quotaMetadataInitialized) {
            reservedQuota = response.getReservedQuota();
            exceededCount = response.getExceededCount();
            unlimitedQuota = response.getUnlimitedQuota();
            quotaLastUpdated = response.hasQuotaLastUpdated() ? response.getQuotaLastUpdated() : null;
            nextQuotaUpdateMessage = response.hasNextQuotaUpdateMessage() ? response.getNextQuotaUpdateMessage() : null;
            quotaMetadataInitialized = true;
        }
        List<DastAuditWorkItem> remainingWorkItems = remainingWorkItems();
        logger.info("DAST audit stream initialized; submitting " + remainingWorkItems.size() + " findings");
        for (DastAuditClientMessage request : prepareAuditRequests(remainingWorkItems, streamId)) {
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
            LOG.debug("Submitting DAST issue {} with request {} on stream {}",
                issueId, requestId, requestStreamId);
            requests.add(DastAuditClientMessage.newBuilder().setAudit(request).build());
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
        }
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
    }

    private void handleError(DastAuditErrorResponse response) {
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
        } else {
            if (completedRequestIds.contains(response.getRequestId())) {
                LOG.debug("Ignoring duplicate DAST audit error for completed request {}", response.getRequestId());
                return;
            }
            fail(new AviatorSimpleException("DAST audit error: " + response.getStatusMessage()));
        }
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
            if (isCurrent()) activeRequestStream = requestStream;
        }

        @Override
        public void onNext(DastAuditServerMessage message) {
            if (!isCurrent()) return;
            switch (message.getResponseTypeCase()) {
                case INIT -> handleInit(message.getInit());
                case AUDIT -> handleAudit(message.getAudit());
                case ERROR -> handleError(message.getError());
                case PONG -> handlePong(message.getPong());
                default -> LOG.warn("Unknown DAST audit response type: {}", message.getResponseTypeCase());
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!isCurrent()) return;
            isRpcCompleted.set(true);
            handleStreamError(throwable);
        }

        @Override
        public void onCompleted() {
            if (!isCurrent()) return;
            isRpcCompleted.set(true);
            stopPingPong();
            LOG.debug("DAST audit stream {} completed with {} terminal responses for {} submitted findings",
                streamId, results.size(), workItems.size());
            if (resultFuture.isDone()) return;
            if (!pendingIssueIds.isEmpty()) {
                handleStreamError(Status.UNAVAILABLE
                    .withDescription("DAST audit stream completed before all findings received terminal responses")
                    .asRuntimeException());
                return;
            }
            completeSuccessfully();
        }
    }

    private void startPingPong() {
        if (pingScheduler == null || pingIntervalSeconds <= 0) return;
        pingTask = pingScheduler.scheduleAtFixedRate(() -> {
            if (isPinging.compareAndSet(false, true)) {
                try {
                    if (requestHandler != null && requestHandler.isReady()) {
                        var ping = DastAuditPingRequest.newBuilder()
                            .setStreamId(streamId)
                            .setTimestamp(System.currentTimeMillis())
                            .build();
                        sendRequest(DastAuditClientMessage.newBuilder().setPing(ping).build());
                    }
                } finally {
                    isPinging.set(false);
                }
            }
        }, pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS);
    }

    private void handleStreamError(Throwable throwable) {
        stopPingPong();
        if (resultFuture.isDone() || isClosed.get() || isRetryScheduled.get()) return;

        if (pendingIssueIds.isEmpty()) {
            completeSuccessfully();
            return;
        }

        if (isRetryableError(throwable) && canRetry(throwable)) {
            if (!isRetryScheduled.compareAndSet(false, true)) return;
            int retryAttempt = streamRetryCount.incrementAndGet();
            long delay = calculateStreamRetryDelay(retryAttempt);
            String maxAttempts = isInfiniteRetryError(throwable)
                ? "infinite" : String.valueOf(Constants.MAX_STREAM_RETRIES);
            logger.info("Retrying DAST audit stream (attempt %d/%s) after %d ms; %d findings remain",
                retryAttempt, maxAttempts, delay, pendingIssueIds.size());
            scheduleRetry(delay);
            return;
        }

        Status status = Status.fromThrowable(throwable);
        completeExceptionally(new AviatorTechnicalException(
            "DAST audit stream failed: " + status.getDescription(), throwable));
    }

    private boolean canRetry(Throwable throwable) {
        int completedCount = workItems.size() - pendingIssueIds.size();
        if (completedCount == lastRetryCompletedCount) {
            if (stagnantRetryCount.incrementAndGet() >= 3) {
                LOG.error("DAST audit stream made no progress after multiple retries");
                return false;
            }
        } else {
            stagnantRetryCount.set(0);
        }
        lastRetryCompletedCount = completedCount;
        return isInfiniteRetryError(throwable) || streamRetryCount.get() < Constants.MAX_STREAM_RETRIES;
    }

    private void scheduleRetry(long delay) {
        Runnable retry = () -> {
            if (!resultFuture.isDone() && !isClosed.get()) {
                isRetryScheduled.set(false);
                startStream();
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
        Status status = Status.fromThrowable(throwable);
        String description = status.getDescription();
        return status.getCode() == Status.Code.UNAVAILABLE ||
            status.getCode() == Status.Code.INTERNAL && description != null &&
                (description.contains("RST_STREAM") || description.contains("PROTOCOL_ERROR"));
    }

    static boolean isInfiniteRetryError(Throwable throwable) {
        Status status = Status.fromThrowable(throwable);
        String description = status.getDescription();
        return status.getCode() == Status.Code.INTERNAL && description != null &&
            description.contains("PROTOCOL_ERROR");
    }

    static long calculateStreamRetryDelay(int retryCount) {
        long delay = (long) (Constants.STREAM_RETRY_BASE_DELAY_MS * Math.pow(2, retryCount - 1));
        return Math.min(delay, Constants.STREAM_RETRY_MAX_DELAY_MS) +
            ThreadLocalRandom.current().nextLong(1000);
    }

    private void sendRequest(DastAuditClientMessage request) {
        RequestHandler<DastAuditClientMessage> currentHandler = requestHandler;
        currentHandler.sendRequest(request).whenComplete((sent, throwable) -> {
            if (currentHandler != requestHandler || resultFuture.isDone() || isClosed.get()) return;
            if (throwable != null || !Boolean.TRUE.equals(sent)) {
                Throwable cause = throwable != null ? throwable : Status.UNAVAILABLE
                    .withDescription("Unable to send DAST audit stream request")
                    .asRuntimeException();
                handleStreamError(cause);
            }
        });
    }

    private void fail(RuntimeException exception) {
        stopPingPong();
        if (requestHandler != null && !requestHandler.isCompleted()) {
            requestHandler.sendError(exception);
        }
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

    private boolean isSuccess(String status) {
        return "SUCCESS".equalsIgnoreCase(status) || "OK".equalsIgnoreCase(status);
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    @Override
    public void close() {
        isClosed.set(true);
        stopPingPong();
        if (retryTask != null) retryTask.cancel(false);
        if (!isRpcCompleted.get() && activeRequestStream != null) {
            activeRequestStream.cancel("DAST audit stream processor closed", null);
        } else if (requestHandler != null && !requestHandler.isCompleted()) {
            requestHandler.sendError(new AviatorTechnicalException("DAST audit stream processor closed"));
        }
        if (resultFuture != null && !resultFuture.isDone()) {
            completeExceptionally(new AviatorTechnicalException("DAST audit stream processor closed"));
        }
    }
}