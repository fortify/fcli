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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.grpc.CorrelationCandidateMatch;
import com.fortify.aviator.grpc.CorrelationClientMessage;
import com.fortify.aviator.grpc.CorrelationErrorResponse;
import com.fortify.aviator.grpc.CorrelationInitResponse;
import com.fortify.aviator.grpc.CorrelationPingRequest;
import com.fortify.aviator.grpc.CorrelationPongResponse;
import com.fortify.aviator.grpc.CorrelationRequest;
import com.fortify.aviator.grpc.CorrelationResponse;
import com.fortify.aviator.grpc.CorrelationServerMessage;
import com.fortify.aviator.grpc.CorrelationServiceGrpc;
import com.fortify.aviator.grpc.CorrelationStreamInitRequest;
import com.fortify.aviator.grpc.CorrelationValidationRequest;
import com.fortify.aviator.grpc.CorrelationValidationResponse;
import com.fortify.aviator.grpc.DastIssueContext;
import com.fortify.aviator.grpc.DastUrlCandidate;
import com.fortify.aviator.grpc.SastCodeFile;
import com.fortify.aviator.grpc.SastCodeLocation;
import com.fortify.aviator.grpc.SastFindingContext;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.grpc.CorrelationStreamState.CandidateMatch;

import io.grpc.Status;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

/**
 * Processes SAST–DAST correlation via a gRPC bidirectional stream to the
 * Aviator server's CorrelationService. Follows the same patterns as
 * {@link AviatorStreamProcessor} for the audit flow.
 *
 * <p>The stream operates in two sequential phases:
 * <ol>
 *   <li><b>Correlation:</b> Send one {@link CorrelationRequest} per SAST finding,
 *       receive {@link CorrelationResponse} with candidate URL matches.</li>
 *   <li><b>Validation:</b> For each candidate match, send a
 *       {@link CorrelationValidationRequest} with full DAST issue context,
 *       receive confirmation/rejection.</li>
 * </ol>
 */
public class CorrelationStreamProcessor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationStreamProcessor.class);
    private static final int MAX_SOURCE_CONTENT_LENGTH = 5000;

    private final AviatorGrpcClient client;
    private final IAviatorLogger logger;
    private final CorrelationServiceGrpc.CorrelationServiceStub asyncStub;
    private final ScheduledExecutorService pingScheduler;
    private final long pingIntervalSeconds;
    private final long defaultTimeoutSeconds;

    private RequestHandler<CorrelationClientMessage> requestHandler;
    private ScheduledFuture<?> pingTask;
    private final AtomicBoolean isPinging = new AtomicBoolean(false);

    private volatile CorrelationStreamState state;
    private CountDownLatch streamLatch;

    // Input data retained for building validation requests
    private List<CorrelationWorkItem> correlationWorkItems;
    private Map<String, List<DastIssue>> urlToDastIssues;
    private final java.util.concurrent.ConcurrentHashMap<String, String> validationRequestToDastId =
        new java.util.concurrent.ConcurrentHashMap<>();
    private volatile CompletableFuture<CorrelationResult> resultFuture;

    /**
     * Keys of SAST–DAST pairs that were confirmed in a previous run and should
     * be skipped during both Phase 1 (correlation) and Phase 2 (validation).
     * Each key is formatted as {@code "sastInstanceId::dastIssueId"}.
     */
    private Set<String> previouslyCorrelatedPairKeys = Set.of();

    public CorrelationStreamProcessor(
            AviatorGrpcClient client,
            IAviatorLogger logger,
            CorrelationServiceGrpc.CorrelationServiceStub asyncStub,
            ScheduledExecutorService pingScheduler,
            long pingIntervalSeconds,
            long defaultTimeoutSeconds) {
        this.client = client;
        this.logger = logger;
        this.asyncStub = asyncStub;
        this.pingScheduler = pingScheduler;
        this.pingIntervalSeconds = pingIntervalSeconds;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    /**
     * Entry point: run correlation on the provided mixed-category buckets.
     * Previously confirmed pairs (from prior runs) are not re-processed.
     *
     * @param config                      stream init configuration (token, app name, etc.)
     * @param mixedBuckets                category buckets containing both SAST and DAST findings
     * @param scanGuid                    SAST scan UUID for building CorrelatedPair results
     * @param previouslyCorrelatedPairKeys keys of already-confirmed pairs to skip, each formatted as
     *                                    {@code "sastInstanceId::dastIssueId"}; may be {@code null}
     * @return future that completes with the list of confirmed correlated pairs
     */
    public CompletableFuture<CorrelationResult> processCorrelation(
            CorrelationStreamConfig config,
            List<? extends Object> mixedBuckets,
            String scanGuid,
            Set<String> previouslyCorrelatedPairKeys) {

        this.previouslyCorrelatedPairKeys =
            previouslyCorrelatedPairKeys != null ? previouslyCorrelatedPairKeys : Set.of();

        // Build URL→DAST map first; needed to evaluate Phase 1 skip eligibility
        this.urlToDastIssues = buildUrlToDastMap(mixedBuckets);
        var workItems = buildCorrelationWorkItems(mixedBuckets);
        this.correlationWorkItems = workItems;

        LOG.debug("Checking URL to DAST issue map");
        urlToDastIssues.forEach((k,v)->LOG.debug(" For url {} no. of dast issues {}", k, v.size()));
        if (workItems.isEmpty()) {
            LOG.info("No SAST findings in mixed buckets; skipping correlation stream.");
            return CompletableFuture.completedFuture(new CorrelationResult(List.of(), List.of(), 0));
        }

        String streamId = UUID.randomUUID().toString();
        this.state = new CorrelationStreamState(streamId, config);
        state.totalCorrelationRequests = workItems.size();

        logger.info("Starting correlation stream — " + workItems.size() + " SAST findings to correlate");

        CompletableFuture<CorrelationResult> resultFuture = new CompletableFuture<>();
        this.resultFuture = resultFuture;
        this.streamLatch = new CountDownLatch(1);

        startStream(resultFuture, scanGuid);

        return resultFuture;
    }

    /**
     * Convenience overload for callers that have no previously confirmed pairs to skip.
     */
    public CompletableFuture<CorrelationResult> processCorrelation(
            CorrelationStreamConfig config,
            List<? extends Object> mixedBuckets,
            String scanGuid) {
        return processCorrelation(config, mixedBuckets, scanGuid, null);
    }

    // ─── Stream lifecycle ──────────────────────────────────────────────

    private void startStream(CompletableFuture<CorrelationResult> resultFuture, String scanGuid) {
        requestHandler = new RequestHandler<>(state.streamId);

        StreamObserver<CorrelationClientMessage> requestObserver =
            asyncStub.processCorrelationStream(new CorrelationResponseObserver(resultFuture, scanGuid));

        requestHandler.initialize(requestObserver);
        startPingPong();

        // Send init message
        var initReq = CorrelationStreamInitRequest.newBuilder()
            .setToken(state.token)
            .setApplicationName(state.applicationName != null ? state.applicationName : "")
            .setStreamId(state.streamId)
            .setRequestId(UUID.randomUUID().toString())
            .setTotalReportedIssues(state.totalCorrelationRequests)
            .setTotalIssuesToCorrelate(state.totalCorrelationRequests);

        if (state.fprBuildId != null) {
            initReq.setFprBuildId(state.fprBuildId);
        }
        if (state.sscApplicationName != null) {
            initReq.setSscApplicationName(state.sscApplicationName);
        }
        if (state.sscApplicationVersion != null) {
            initReq.setSscApplicationVersion(state.sscApplicationVersion);
        }

        requestHandler.sendRequest(
            CorrelationClientMessage.newBuilder().setInit(initReq.build()).build()
        );
    }

    // ─── Phase 1: Send correlation requests ────────────────────────────

    private void sendCorrelationRequests() {
        state.currentPhase = CorrelationStreamState.Phase.CORRELATING;
        logger.info("Sending " + correlationWorkItems.size() + " correlation requests...");

        for (var item : correlationWorkItems) {
            var req = buildCorrelationRequest(state.streamId, item);
            requestHandler.sendRequest(
                CorrelationClientMessage.newBuilder().setCorrelation(req).build()
            );
            state.sentCorrelations.incrementAndGet();
        }
    }

    // ─── Phase 2: Send validation requests ─────────────────────────────

    private void transitionToValidation(String scanGuid) {
        state.currentPhase = CorrelationStreamState.Phase.VALIDATING;

        var validationItems = buildValidationWorkItems();
        state.totalValidationRequests = validationItems.size();

        if (validationItems.isEmpty()) {
            logger.info("No candidates to validate. Completing stream.");
            if (!resultFuture.isDone()) {
                resultFuture.complete(new CorrelationResult(
                    new ArrayList<>(state.confirmedPairs),
                    new ArrayList<>(state.rejectedPairs),
                    state.successfulCorrelations.get()
                ));
            }
            requestHandler.complete();
            streamLatch.countDown();
            return;
        }

        logger.info("Sending " + validationItems.size() + " validation requests...");
        for (var item : validationItems) {
            var req = buildValidationRequest(state.streamId, item);
            validationRequestToDastId.put(req.getRequestId(),
                item.dastIssue().getId() != null ? item.dastIssue().getId() : "");
            requestHandler.sendRequest(
                CorrelationClientMessage.newBuilder().setValidation(req).build()
            );
            state.sentValidations.incrementAndGet();
        }
    }

    // ─── Response observer ─────────────────────────────────────────────

    private class CorrelationResponseObserver
            implements ClientResponseObserver<CorrelationClientMessage, CorrelationServerMessage> {

        private final CompletableFuture<CorrelationResult> resultFuture;
        private final String scanGuid;

        CorrelationResponseObserver(CompletableFuture<CorrelationResult> resultFuture, String scanGuid) {
            this.resultFuture = resultFuture;
            this.scanGuid = scanGuid;
        }

        @Override
        public void beforeStart(ClientCallStreamObserver<CorrelationClientMessage> requestStream) {
            // No special setup needed; RequestHandler manages the stream
        }

        @Override
        public void onNext(CorrelationServerMessage message) {
            try {
                switch (message.getResponseTypeCase()) {
                    case INIT -> handleInitResponse(message.getInit());
                    case CORRELATION -> handleCorrelationResponse(message.getCorrelation());
                    case VALIDATION -> handleValidationResponse(message.getValidation(), scanGuid);
                    case ERROR -> handleErrorResponse(message.getError(), resultFuture);
                    case PONG -> handlePongResponse(message.getPong());
                    default -> LOG.warn("Unknown correlation response type: {}", message.getResponseTypeCase());
                }
            } catch (Exception e) {
                LOG.error("Error handling correlation response", e);
            }
        }

        @Override
        public void onError(Throwable t) {
            stopPingPong();
            Status status = Status.fromThrowable(t);
            LOG.error("Correlation stream error: {} - {}", status.getCode(), status.getDescription(), t);
            resultFuture.completeExceptionally(
                new AviatorTechnicalException("Correlation stream failed: " + status.getDescription(), t)
            );
            streamLatch.countDown();
        }

        @Override
        public void onCompleted() {
            stopPingPong();
            state.currentPhase = CorrelationStreamState.Phase.COMPLETE;
            logger.info("Correlation stream completed — " + state.confirmedPairs.size() + " confirmed pairs");
            if (!resultFuture.isDone()) {
                resultFuture.complete(new CorrelationResult(
                    new ArrayList<>(state.confirmedPairs),
                    new ArrayList<>(state.rejectedPairs),
                    state.successfulCorrelations.get()
                ));
            }
            streamLatch.countDown();
        }
    }

    // ─── Response handlers ─────────────────────────────────────────────

    private void handleInitResponse(CorrelationInitResponse resp) {
        LOG.info("Correlation stream initialized. Server stream ID: {}, Reserved quota: {}, Status: {}",
            resp.getServerStreamId(), resp.getReservedQuota(), resp.getStatus());

        state.isStreamInitialized = true;
        state.quota = resp.getReservedQuota();

        if (!"OK".equalsIgnoreCase(resp.getStatus()) && !"SUCCESS".equalsIgnoreCase(resp.getStatus())) {
            LOG.warn("Init response status: {} — {}", resp.getStatus(), resp.getStatusMessage());
        }

        // Start sending correlation requests
        sendCorrelationRequests();
    }

    private void handleCorrelationResponse(CorrelationResponse resp) {
        int received = state.receivedCorrelations.incrementAndGet();
        LOG.debug("Correlation response {}/{} for SAST {}: status={}",
            received, state.totalCorrelationRequests, resp.getSastId(), resp.getStatus());

        logger.progress("Correlating " + received + " of " + state.totalCorrelationRequests + " SAST findings");

        if ("OK".equalsIgnoreCase(resp.getStatus()) || "SUCCESS".equalsIgnoreCase(resp.getStatus())) {
            state.successfulCorrelations.incrementAndGet();
            for (CorrelationCandidateMatch match : resp.getMatchesList()) {
                state.candidateMatches.add(new CandidateMatch(
                    resp.getSastId(),
                    match.getUrl(),
                    match.getConfidence(),
                    match.getRationale()
                ));
            }
        } else {
            LOG.debug("Non-OK correlation for SAST {}: {} — {}",
                resp.getSastId(), resp.getStatus(), resp.getNoCorrelationReason());
        }

        // Check if all correlation responses received → transition
        if (received >= state.totalCorrelationRequests) {
            logger.info("All " + received + " correlation responses received. " +
                state.candidateMatches.size() + " candidate matches found.");
            // scanGuid is captured in the observer
            transitionToValidation(null); // scanGuid will come from observer; use state
        }
    }

    private void handleValidationResponse(CorrelationValidationResponse resp, String scanGuid) {
        int received = state.receivedValidations.incrementAndGet();
        LOG.info("Validation response {}/{} for SAST {}: confirmed={}",
            received, state.totalValidationRequests, resp.getSastId(),
            resp.hasDecision() && resp.getDecision().getConfirmed());

        logger.progress("Validating " + received + " of " + state.totalValidationRequests + " correlation candidates");

        String dastIssueId = findDastIssueIdForValidation(resp);
        if (resp.hasDecision() && resp.getDecision().getConfirmed()) {
            if (dastIssueId != null) {
                state.confirmedPairs.add(new CorrelatedPair(
                    resp.getSastId(),
                    dastIssueId,
                    scanGuid != null ? scanGuid : "",
                    resp.getDecision().getConfidence(),
                    resp.getDecision().getRationale()
                ));
            }
        } else {
            // Record rejected pair so the caller can persist it in the SAST FPR
            if (dastIssueId != null && !dastIssueId.isEmpty()) {
                state.rejectedPairs.add(new CorrelatedPair(
                    resp.getSastId(),
                    dastIssueId,
                    scanGuid != null ? scanGuid : "",
                    resp.hasDecision() ? resp.getDecision().getConfidence() : "",
                    resp.hasDecision() ? resp.getDecision().getRationale() : "rejected"
                ));
            }
        }

        // Check if all validations received → complete
        if (received >= state.totalValidationRequests) {
            logger.info("All " + received + " validation responses received. " +
                state.confirmedPairs.size() + " confirmed pairs, " +
                state.rejectedPairs.size() + " rejected pairs.");
            if (!resultFuture.isDone()) {
                resultFuture.complete(new CorrelationResult(
                    new ArrayList<>(state.confirmedPairs),
                    new ArrayList<>(state.rejectedPairs),
                    state.successfulCorrelations.get()
                ));
            }
            requestHandler.complete();
            streamLatch.countDown();
        }
    }

    private void handleErrorResponse(CorrelationErrorResponse resp,
                                      CompletableFuture<CorrelationResult> resultFuture) {
        LOG.error("Correlation stream error from server: {} — {}",
            resp.getStatus(), resp.getStatusMessage());
        resultFuture.completeExceptionally(
            new AviatorSimpleException("Correlation error: " + resp.getStatusMessage())
        );
        streamLatch.countDown();
    }

    private void handlePongResponse(CorrelationPongResponse pong) {
        long latency = System.currentTimeMillis() - pong.getClientTimestamp();
        LOG.debug("Correlation pong received. Latency: {} ms", latency);
    }

    // ─── Ping/pong keepalive ───────────────────────────────────────────

    private void startPingPong() {
        if (pingIntervalSeconds <= 0 || pingScheduler == null) return;

        pingTask = pingScheduler.scheduleAtFixedRate(() -> {
            if (isPinging.compareAndSet(false, true)) {
                try {
                    if (requestHandler != null && requestHandler.isReady()) {
                        var ping = CorrelationPingRequest.newBuilder()
                            .setStreamId(state.streamId)
                            .setTimestamp(System.currentTimeMillis())
                            .build();
                        requestHandler.sendRequest(
                            CorrelationClientMessage.newBuilder().setPing(ping).build()
                        );
                    }
                } finally {
                    isPinging.set(false);
                }
            }
        }, pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS);
    }

    private void stopPingPong() {
        if (pingTask != null && !pingTask.isCancelled()) {
            pingTask.cancel(false);
        }
    }

    // ─── Request builders ──────────────────────────────────────────────

    private CorrelationRequest buildCorrelationRequest(String streamId, CorrelationWorkItem item) {
        var sastFinding = buildSastFindingContext(item.vuln());

        var builder = CorrelationRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setStreamId(streamId)
            .setCategory(item.category())
            .setSastFinding(sastFinding);

        for (String url : item.dastUrls()) {
            builder.addDastUrlCandidates(DastUrlCandidate.newBuilder().setUrl(url).build());
        }

        return builder.build();
    }

    private CorrelationValidationRequest buildValidationRequest(String streamId, ValidationWorkItem item) {
        var sastFinding = buildSastFindingContext(item.vuln());

        var dastContext = DastIssueContext.newBuilder()
            .setDastIssueId(item.dastIssue().getId() != null ? item.dastIssue().getId() : "")
            .setIssueName(item.dastIssue().getName() != null ? item.dastIssue().getName() : "")
            .setSeverity(String.valueOf(item.dastIssue().getSeverity()))
            .setUrl(item.dastIssue().getSessionUrl() != null ? item.dastIssue().getSessionUrl() : "")
            .setCweId(item.dastIssue().getCweId() != null ? item.dastIssue().getCweId() : "")
            .setSummary(item.dastIssue().getSummary() != null ? item.dastIssue().getSummary() : "");

        if (item.dastIssue().getReproStepUrls() != null) {
            dastContext.addAllReproStepUrls(item.dastIssue().getReproStepUrls());
        }

        return CorrelationValidationRequest.newBuilder()
            .setRequestId(UUID.randomUUID().toString())
            .setStreamId(streamId)
            .setCategory(item.category())
            .setSastFinding(sastFinding)
            .setDastIssue(dastContext.build())
            .build();
    }

    private SastFindingContext buildSastFindingContext(Vulnerability vuln) {
        var builder = SastFindingContext.newBuilder()
            .setInstanceId(vuln.getInstanceID() != null ? vuln.getInstanceID() : "")
            .setCategory(vuln.getType() != null ? vuln.getType() : "")
            .setType(vuln.getType() != null ? vuln.getType() : "")
            .setSubType(vuln.getSubType() != null ? vuln.getSubType() : "")
            .setShortDescription(vuln.getShortDescription() != null ? vuln.getShortDescription() : "");

        if (vuln.getSource() != null) {
            builder.setSource(SastCodeLocation.newBuilder()
                .setFilename(vuln.getSource().getFilename() != null ? vuln.getSource().getFilename() : "")
                .setLine(vuln.getSource().getLine())
                .setCode(vuln.getSource().getCode() != null ? vuln.getSource().getCode() : "")
                .build());
        }

        if (vuln.getSink() != null) {
            builder.setSink(SastCodeLocation.newBuilder()
                .setFilename(vuln.getSink().getFilename() != null ? vuln.getSink().getFilename() : "")
                .setLine(vuln.getSink().getLine())
                .setCode(vuln.getSink().getCode() != null ? vuln.getSink().getCode() : "")
                .build());
        }

        if (vuln.getFiles() != null) {
            for (var file : vuln.getFiles()) {
                String content = file.getContent();
                if (content != null && content.length() > MAX_SOURCE_CONTENT_LENGTH) {
                    content = content.substring(0, MAX_SOURCE_CONTENT_LENGTH);
                }
                builder.addFiles(SastCodeFile.newBuilder()
                    .setName(file.getName() != null ? file.getName() : "")
                    .setContent(content != null ? content : "")
                    .build());
            }
        }

        if (vuln.getFiles() != null && !vuln.getFiles().isEmpty()) {
            builder.setFullSourceFileName(vuln.getFiles().get(0).getName() != null
                ? vuln.getFiles().get(0).getName() : "");
        }

        return builder.build();
    }

    // ─── Work item construction ────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<CorrelationWorkItem> buildCorrelationWorkItems(List<? extends Object> mixedBuckets) {
        List<CorrelationWorkItem> items = new ArrayList<>();

        for (Object bucketObj : mixedBuckets) {
            if (bucketObj instanceof CorrelationBucketData data) {
                Set<String> dastUrls = new HashSet<>();
                for (var dastIssue : data.dastFindings()) {
                    if (dastIssue.getSessionUrl() != null && !dastIssue.getSessionUrl().isEmpty()) {
                        dastUrls.add(dastIssue.getSessionUrl());
                    }
                }
                if (dastUrls.isEmpty()) continue;
                List<String> urlList = new ArrayList<>(dastUrls);
                for (Vulnerability vuln : data.sastFindings()) {
                    // Strip URLs where every mapped DAST issue is already confirmed with this SAST finding
                    List<String> newUrls = filterNewUrls(vuln.getInstanceID(), urlList);
                    if (newUrls.isEmpty()) {
                        LOG.debug("Skipping SAST finding {} from Phase 1 — all reachable DAST issues already confirmed",
                            vuln.getInstanceID());
                        continue;
                    }
                    items.add(new CorrelationWorkItem(data.category(), vuln, newUrls));
                }
            }
        }

        return items;
    }

    /**
     * Returns the subset of {@code urls} for which at least one mapped DAST issue
     * is NOT yet confirmed with {@code sastInstanceId}.
     *
     * <ul>
     *   <li>URLs with no mapped DAST issues are kept (the server may resolve them).</li>
     *   <li>URLs where every mapped DAST issue is already in
     *       {@link #previouslyCorrelatedPairKeys} are excluded — they add no new work.</li>
     * </ul>
     */
    private List<String> filterNewUrls(String sastInstanceId, List<String> urls) {
        if (previouslyCorrelatedPairKeys.isEmpty()) return urls; // fast path: nothing confirmed yet
        List<String> result = new ArrayList<>();
        for (String url : urls) {
            List<DastIssue> issues = urlToDastIssues.getOrDefault(url, List.of());
            if (issues.isEmpty()) {
                result.add(url); // no mapping known locally — let server decide
                continue;
            }
            boolean hasUncorrelated = issues.stream()
                .filter(d -> d.getId() != null && !d.getId().isEmpty())
                .anyMatch(d -> !previouslyCorrelatedPairKeys.contains(sastInstanceId + "::" + d.getId()));
            if (hasUncorrelated) {
                result.add(url);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, List<DastIssue>> buildUrlToDastMap(List<? extends Object> mixedBuckets) {
        var map = new java.util.HashMap<String, List<DastIssue>>();
        for (Object bucketObj : mixedBuckets) {
            if (bucketObj instanceof CorrelationBucketData data) {
                for (DastIssue issue : data.dastFindings()) {
                    String url = issue.getSessionUrl();
                    if (url != null && !url.isEmpty()) {
                        map.computeIfAbsent(url, k -> new ArrayList<>()).add(issue);
                    }
                }
            }
        }
        return map;
    }

    private List<ValidationWorkItem> buildValidationWorkItems() {
        List<ValidationWorkItem> items = new ArrayList<>();

        for (CandidateMatch match : state.candidateMatches) {
            LOG.debug("Match URL {}", match.url());

            List<DastIssue> issues = urlToDastIssues.get(match.url());
            if (issues == null || issues.isEmpty()) continue;

            // Find the original Vulnerability for this SAST instanceId
            Vulnerability vuln = findVulnerabilityById(match.sastInstanceId());
            if (vuln == null) continue;

            String category = findCategoryForSast(match.sastInstanceId());

            for (DastIssue dastIssue : issues) {
                String pairKey = match.sastInstanceId() + "::" + dastIssue.getId();
                if (previouslyCorrelatedPairKeys.contains(pairKey)) {
                    LOG.debug("Skipping already confirmed pair sast={} dast={} from Phase 2 validation",
                        match.sastInstanceId(), dastIssue.getId());
                    continue;
                }
                items.add(new ValidationWorkItem(category, vuln, dastIssue));
            }
        }

        return items;
    }

    private Vulnerability findVulnerabilityById(String instanceId) {
        if (correlationWorkItems == null) return null;
        return correlationWorkItems.stream()
            .filter(item -> instanceId.equals(item.vuln().getInstanceID()))
            .map(CorrelationWorkItem::vuln)
            .findFirst()
            .orElse(null);
    }

    private String findCategoryForSast(String instanceId) {
        if (correlationWorkItems == null) return "";
        return correlationWorkItems.stream()
            .filter(item -> instanceId.equals(item.vuln().getInstanceID()))
            .map(CorrelationWorkItem::category)
            .findFirst()
            .orElse("");
    }


    private String findDastIssueIdForValidation(CorrelationValidationResponse resp) {
        // Use the dastId field added to CorrelationValidationResponse proto,
        // falling back to the requestId-to-dastIssueId tracking map
        String dastId = resp.getDastId();
        if (dastId != null && !dastId.isEmpty()) {
            return dastId;
        }
        return validationRequestToDastId.get(resp.getRequestId());
    }

    // ─── Data transfer records ─────────────────────────────────────────

    record CorrelationWorkItem(String category, Vulnerability vuln, List<String> dastUrls) {}

    record ValidationWorkItem(String category, Vulnerability vuln, DastIssue dastIssue) {}

    /**
     * Interface for passing bucket data from the fcli-aviator module
     * into this fcli-aviator-common processor without a direct dependency
     * on {@code CategoryBucket}.
     */
    public record CorrelationBucketData(
        String category,
        List<Vulnerability> sastFindings,
        List<DastIssue> dastFindings
    ) {}

    // ─── Cleanup ───────────────────────────────────────────────────────

    @Override
    public void close() {
        stopPingPong();
        if (requestHandler != null && !requestHandler.isCompleted()) {
            requestHandler.complete();
        }
    }
}
