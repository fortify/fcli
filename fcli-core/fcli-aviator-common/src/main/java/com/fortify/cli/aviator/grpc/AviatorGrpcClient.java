package com.fortify.cli.aviator.grpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.application.Application;
import com.fortify.aviator.application.ApplicationById;
import com.fortify.aviator.application.ApplicationByTenantName;
import com.fortify.aviator.application.ApplicationList;
import com.fortify.aviator.application.ApplicationResponseMessage;
import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.aviator.application.CreateApplicationRequest;
import com.fortify.aviator.application.UpdateApplicationRequest;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.aviator.entitlement.EntitlementServiceGrpc;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantRequest;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantResponse;
import com.fortify.aviator.grpc.AnalysisInfo;
import com.fortify.aviator.grpc.AuditRequest;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.File;
import com.fortify.aviator.grpc.Fragment;
import com.fortify.aviator.grpc.IssueData;
import com.fortify.aviator.grpc.PingRequest;
import com.fortify.aviator.grpc.StackTraceElementList;
import com.fortify.aviator.grpc.StreamInitRequest;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.core.model.StackTraceElement;
import com.fortify.cli.aviator.core.model.UserPrompt;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.StringUtil;
import com.fortify.grpc.token.DeleteTokenRequest;
import com.fortify.grpc.token.DeleteTokenResponse;
import com.fortify.grpc.token.ListTokensRequest;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.RevokeTokenRequest;
import com.fortify.grpc.token.RevokeTokenResponse;
import com.fortify.grpc.token.TokenGenerationRequest;
import com.fortify.grpc.token.TokenGenerationResponse;
import com.fortify.grpc.token.TokenServiceGrpc;
import com.fortify.grpc.token.TokenValidationRequest;
import com.fortify.grpc.token.TokenValidationResponse;

import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import io.grpc.stub.StreamObserver;

public class AviatorGrpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClient.class);

    private final IAviatorLogger logger;

    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    private static final int INITIAL_REQUEST_WINDOW = 100;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final long MAX_DELAY_MS = 2000;


    private final Map<String, RequestMetrics> requestMetricsMap = new ConcurrentHashMap<>();


    private final AtomicInteger consecutiveBackpressureViolations = new AtomicInteger(0);
    private final AtomicLong lastBackpressureViolation = new AtomicLong(0);
    private final AtomicInteger currentBackoff = new AtomicInteger(1);
    private final AtomicInteger serverWindowSize = new AtomicInteger(INITIAL_REQUEST_WINDOW);

    private final CountDownLatch latch = new CountDownLatch(1);
    private final ManagedChannel channel;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final ApplicationServiceGrpc.ApplicationServiceBlockingStub blockingStub;
    private final TokenServiceGrpc.TokenServiceBlockingStub tokenServiceBlockingStub;
    private final EntitlementServiceGrpc.EntitlementServiceBlockingStub entitlementServiceBlockingStub;
    private final String streamId;
    private final long defaultTimeoutSeconds;
    private final ExecutorService processingExecutor;
    private final AtomicBoolean isShutdown;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final Semaphore requestSemaphore;
    private final AtomicInteger outstandingRequests = new AtomicInteger(0);
    private volatile StreamObserver<UserPromptRequest> requestObserver;
    private final AtomicBoolean streamCompleted = new AtomicBoolean(false);
    private volatile boolean isStreamActive = false;

    private final ScheduledExecutorService pingScheduler;
    private ScheduledFuture<?> pingTask;
    private final long pingIntervalSeconds;
    private final AtomicBoolean isPinging = new AtomicBoolean(false);

    private static class RequestMetrics {
        private final long startTime;
        private volatile long endTime = 0;
        private volatile String status = "PENDING";

        public RequestMetrics() {
            this.startTime = System.currentTimeMillis();
        }

        public void complete(String status) {
            this.endTime = System.currentTimeMillis();
            this.status = status;
        }

        public long getDuration() {
            return endTime > 0 ? endTime - startTime : System.currentTimeMillis() - startTime;
        }
    }

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        LOG.info("Initializing AviatorGrpcClient with ManagedChannel");
        this.logger = logger;
        this.streamId = UUID.randomUUID().toString();
        this.channel = channel;

        this.asyncStub = AuditorServiceGrpc.newStub(channel)
                .withCompression("gzip")
                .withMaxInboundMessageSize(MAX_MESSAGE_SIZE)
                .withMaxOutboundMessageSize(MAX_MESSAGE_SIZE)
                .withWaitForReady();

        this.blockingStub = ApplicationServiceGrpc.newBlockingStub(channel)
                .withCompression("gzip")
                .withMaxInboundMessageSize(MAX_MESSAGE_SIZE)
                .withMaxOutboundMessageSize(MAX_MESSAGE_SIZE)
                .withWaitForReady();

        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel)
                .withCompression("gzip")
                .withMaxInboundMessageSize(MAX_MESSAGE_SIZE)
                .withMaxOutboundMessageSize(MAX_MESSAGE_SIZE)
                .withWaitForReady();

        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel)
                .withCompression("gzip")
                .withMaxInboundMessageSize(MAX_MESSAGE_SIZE)
                .withMaxOutboundMessageSize(MAX_MESSAGE_SIZE)
                .withWaitForReady();

        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.processingExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "aviator-client-processing-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
        this.isShutdown = new AtomicBoolean(false);
        this.requestSemaphore = new Semaphore(INITIAL_REQUEST_WINDOW);

        this.pingIntervalSeconds = pingIntervalSeconds;

        this.pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "aviator-client-ping-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    public AviatorGrpcClient(String host, int port, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port)
                        .useTransportSecurity()
                        .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                        .keepAliveTime(30, TimeUnit.SECONDS)
                        .keepAliveTimeout(10, TimeUnit.SECONDS)
                        .keepAliveWithoutCalls(true)
                        .enableRetry()
                        .compressorRegistry(CompressorRegistry.getDefaultInstance())
                        .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                        .build(),
                defaultTimeoutSeconds,
                logger, pingIntervalSeconds);
        LOG.info("Initialized AviatorGrpcClient - Host: {}, Port: {}", host, port);
    }
    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger) {
        this(channel, defaultTimeoutSeconds, logger, 30);
    }

    private void startPingPong() {
        if (isPinging.compareAndSet(false, true)) {
            logger.info("Starting ping-pong keepalive with interval of {} seconds", pingIntervalSeconds);
            pingTask = pingScheduler.scheduleAtFixedRate(
                    this::sendPing,
                    pingIntervalSeconds,
                    pingIntervalSeconds,
                    TimeUnit.SECONDS
            );
        }
    }

    private void stopPingPong() {
        if (isPinging.compareAndSet(true, false) && pingTask != null) {
            logger.info("Stopping ping-pong keepalive");
            pingTask.cancel(false);
            pingTask = null;
        }
    }

    // Send ping message
    private void sendPing() {
        try {
            if (requestObserver != null && !streamCompleted.get() && isStreamActive) {
                PingRequest pingRequest = PingRequest.newBuilder()
                        .setStreamId(streamId)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                UserPromptRequest pingMsg = UserPromptRequest.newBuilder()
                        .setPing(pingRequest)
                        .build();

                LOG.info("Sending ping streamId: {}", streamId);
                requestObserver.onNext(pingMsg);
            }
        } catch (Exception e) {
            if (!streamCompleted.get()) {
                LOG.warn("Failed to send ping: {}", e.getMessage());
            }
        }
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(
            Queue<UserPrompt> requests, String projectName, String token) {
        isStreamActive = true;
        if (requests == null || requests.isEmpty()) {
            LOG.info("No issues to process");
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        logger.info("Starting processing - Total Issues: " + requests.size());
        CompletableFuture<Map<String, AuditResponse>> resultFuture = new CompletableFuture<>();
        Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        AtomicInteger processedRequests = new AtomicInteger(0);
        int totalRequests = requests.size();
        AtomicInteger errorRequests = new AtomicInteger(0);

        ClientResponseObserver<UserPromptRequest, AuditorResponse> responseObserver =
                new ClientResponseObserver<>() {
                    private final AtomicBoolean isInitialized = new AtomicBoolean(false);

                    @Override
                    public void beforeStart(ClientCallStreamObserver<UserPromptRequest> requestStream) {
                        requestObserver = requestStream;
                    }

                    @Override
                    public void onNext(AuditorResponse response) {
                        logger.info("Received response - Status: " + response.getStatus() + ", RequestId: " + response.getRequestId());
                        if ("PONG".equals(response.getStatus())) {
                            logger.info("Received pong from server: StreamId: {}, Client timestamp: {}, Server timestamp: {}, RequestId: {}",
                                    response.getStreamId(),
                                    response.getPong().getClientTimestamp(),
                                    response.getPong().getServerTimestamp(),
                                    response.getRequestId());
                            return;
                        }

                        if ("PROCESSING_ERROR".equals(response.getStatus()) || "REQUEST_PROCESSING_ERROR".equals(response.getStatus())) {
                            logger.error(response.getStatusMessage());
                            RequestMetrics metrics = requestMetricsMap.remove(response.getRequestId());
                            if (metrics != null) {
                                metrics.complete("ERROR");
                            }
                            errorRequests.incrementAndGet();
                            outstandingRequests.decrementAndGet();
                            requestSemaphore.release();
                            return;
                        }

                        if ("INTERNAL_ERROR".equals(response.getStatus())) {
                            String cliMessage = "Internal server error occurred";
                            logger.error(cliMessage);
                            resultFuture.completeExceptionally(new AviatorTechnicalException(cliMessage));
                            if (requestObserver != null) {
                                requestObserver.onCompleted();
                            }
                            streamCompleted.set(true);
                            latch.countDown();
                            return;
                        }

                        if ("BACKPRESSURE_WARNING".equals(response.getStatus())) {
                            handleBackpressureWarning();
                        } else if ("BACKPRESSURE_VIOLATION".equals(response.getStatus())) {
                            logger.error("Server terminated stream due to backpressure violations: {}", response.getStatusMessage());
                            streamCompleted.set(true);
                            if (!resultFuture.isDone()) {
                                resultFuture.completeExceptionally(new AviatorTechnicalException("Stream terminated by server: " + response.getStatusMessage()));
                            }
                            return;
                        } else {
                            consecutiveBackpressureViolations.set(0);
                            currentBackoff.set(1);
                        }

                        RequestMetrics metrics = requestMetricsMap.remove(response.getRequestId());
                        if (metrics != null) {
                            metrics.complete(response.getStatus());
                            logger.info("Request {} completed with status {} in {}ms",
                                    response.getRequestId(), response.getStatus(), metrics.getDuration());
                        }

                        if (!isInitialized.get()) {
                            if ("SUCCESS".equals(response.getStatus())) {
                                isInitialized.set(true);
                                initLatch.countDown();
                                logger.info("Stream initialized successfully");
                                startPingPong();
                            } else {
                                logger.progress("Stream initialization failed: " + response.getStatusMessage());
                                resultFuture.completeExceptionally(new AviatorTechnicalException("Stream initialization failed: " + response.getStatusMessage()));
                                if (requestObserver != null) {
                                    requestObserver.onCompleted();
                                }
                                latch.countDown();
                            }
                        } else {
                            AuditResponse auditResponse = convertToAuditResponse(response);
                            responses.put(response.getRequestId(), auditResponse);
                            int completed = processedRequests.incrementAndGet();
                            outstandingRequests.decrementAndGet();
                            requestSemaphore.release();

                            logger.progress("Processed " + completed + " out of " + totalRequests + " issues");

                            if (completed + errorRequests.get() >= totalRequests) {
                                logger.info("All requests processed, completing stream");
                                if (streamCompleted.compareAndSet(false, true) && requestObserver != null) {
                                    requestObserver.onCompleted();
                                }
                                if (!resultFuture.isDone()) {
                                    resultFuture.complete(responses);
                                }
                                latch.countDown();
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable t) {
                        stopPingPong();
                        if (!resultFuture.isDone()) {
                            if (t instanceof StatusRuntimeException sre) {
                                String description = sre.getStatus().getDescription() != null ? sre.getStatus().getDescription() : "Unknown gRPC error";
                                String techMessage = String.format("gRPC stream failed: %s (Status: %s)", description, sre.getStatus().getCode());
                                resultFuture.completeExceptionally(new AviatorTechnicalException(techMessage, t));
                            } else {
                                resultFuture.completeExceptionally(new AviatorTechnicalException("Stream error", t));
                            }
                        }
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        stopPingPong();
                        logger.progress("Stream completed");
                        if (!resultFuture.isDone()) {
                            resultFuture.complete(responses);
                        }
                        latch.countDown();
                    }
                };

        asyncStub.processStream(responseObserver);

        try {
            LOG.info("Sending initialization request");
            String initRequestId = UUID.randomUUID().toString();
            UserPromptRequest initRequest = UserPromptRequest.newBuilder()
                    .setInit(StreamInitRequest.newBuilder()
                            .setStreamId(streamId)
                            .setRequestId(initRequestId)
                            .setToken(token)
                            .setApplicationName(projectName)
                            .setTotalReportedIssues(totalRequests)
                            .setTotalIssuesToPredict(totalRequests)
                            .build())
                    .build();

            requestObserver.onNext(initRequest);
            LOG.info("Client Id  for stream initialization {}",streamId);

            processingExecutor.submit(() -> {
                try {
                    if (!initLatch.await(30, TimeUnit.SECONDS)) {
                        throw new AviatorTechnicalException("Stream initialization timed out");
                    }
                    processRequests(requests, requestObserver);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AviatorTechnicalException("Interrupted during request processing", e);
                } catch (Exception e) {
                    if (!streamCompleted.get()) {
                        throw new AviatorTechnicalException("Error during request processing execution", e);
                    }
                    LOG.warn("Exception caught after stream completion during processing execution", e);
                }
            });
        } catch (Exception e) {
            if (requestObserver != null) {
                requestObserver.onError(e);
            }
            throw new AviatorTechnicalException("Error initiating batch processing", e);
        }

        return resultFuture.exceptionally(ex -> {
            stopPingPong();
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) && ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof AviatorSimpleException) throw (AviatorSimpleException) cause;
            if (cause instanceof AviatorTechnicalException) throw (AviatorTechnicalException) cause;
            throw new AviatorTechnicalException("Processing FPR failed", cause);
        });
    }

    private void processRequests(Queue<UserPrompt> requests, StreamObserver<UserPromptRequest> observer) {
        logger.progress("Starting to process issues...");
        AtomicInteger failedRequests = new AtomicInteger(0);
        AtomicInteger pendingRequests = new AtomicInteger(0);

        while (!requests.isEmpty() && !isShutdown.get() && !streamCompleted.get()) {
            boolean acquiredSemaphore = false;
            try {
                requestSemaphore.acquire();
                acquiredSemaphore = true;
                if (currentBackoff.get() > 1) {
                    int backoffMs = (int) (BASE_DELAY_MS * currentBackoff.get());
                    logger.info("Applying backoff delay of {}ms", backoffMs);
                    Thread.sleep(backoffMs);
                }

                while (pendingRequests.get() >= serverWindowSize.get() * 0.9 && !isShutdown.get() && !streamCompleted.get()) {
                    Thread.sleep(50);
                }

                if (streamCompleted.get()) {
                    break;
                }

                requestSemaphore.acquire();
                UserPrompt request = requests.poll();
                if (request == null) break;

                String requestId = UUID.randomUUID().toString();
                AuditRequest auditRequest = convertToAuditRequest(request, streamId, requestId);
                UserPromptRequest promptRequest = UserPromptRequest.newBuilder()
                        .setAudit(auditRequest)
                        .build();

                int messageSize = promptRequest.getSerializedSize();
                LOG.debug("Request size: {} bytes", messageSize);

                if (messageSize > MAX_MESSAGE_SIZE) {
                    LOG.warn("Request too large, skipping");
                    throw new AviatorSimpleException("Request size exceeds maximum allowed limit");
                }

                requestMetricsMap.put(requestId, new RequestMetrics());
                pendingRequests.incrementAndGet();

                boolean sent = sendRequestWithRetry(promptRequest, MAX_RETRIES);
                if (!sent) {
                    requestMetricsMap.remove(requestId);
                    pendingRequests.decrementAndGet();
                    failedRequests.incrementAndGet();
                    LOG.error("Failed to send request after retries: {}", requestId);
                    if (failedRequests.get() > 10) {
                        throw new AviatorTechnicalException("Too many failed requests");
                    }
                } else {
                    outstandingRequests.incrementAndGet();
                }

                pendingRequests.decrementAndGet();
            } catch (InterruptedException ie) {
                if (!streamCompleted.get()) {
                    Thread.currentThread().interrupt();
                    throw new AviatorTechnicalException("Thread interrupted while processing requests", ie);
                }
                break;
            } catch (AviatorSimpleException e) {
                throw e;
            } catch (Exception e) {
                if (!streamCompleted.get()) {
                    LOG.error("Error processing request: {}", e.getMessage(), e);
                    throw new AviatorTechnicalException("Error processing request", e);
                }
            } finally {
                if (acquiredSemaphore) {
                    requestSemaphore.release();
                }
            }
        }
    }

    private boolean sendRequestWithRetry(UserPromptRequest request, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (requestObserver == null) {
                    LOG.debug("Request observer is null, aborting send");
                    return false;
                }

                if (streamCompleted.get()) {
                    return false;
                }

                int messageSize = request.getSerializedSize();
                if (messageSize > MAX_MESSAGE_SIZE) {
                    LOG.error("Message size too large: {} bytes", messageSize);
                    throw new AviatorSimpleException("Message size exceeds maximum allowed limit");
                }

                requestObserver.onNext(request);
                return true;
            } catch (AviatorSimpleException e) {
                throw e; // Propagate user-facing errors
            } catch (Exception e) {
                if (!streamCompleted.get()) {
                    LOG.error("Error sending request (attempt {}): {}", attempt + 1, e.getMessage());
                }
                if (attempt == maxRetries - 1) {
                    return false;
                }
                try {
                    long baseBackoff = BASE_DELAY_MS * Math.min(10, currentBackoff.get() * (attempt + 1));
                    long jitter = ThreadLocalRandom.current().nextLong(100);
                    long backoffMs = Math.min(MAX_DELAY_MS, baseBackoff) + jitter;
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new AviatorTechnicalException("Interrupted during retry backoff", ie);
                }
            }
        }
        return false;
    }

    /**
     * Handles backpressure warnings from the server
     */
    private void handleBackpressureWarning() {
        long now = System.currentTimeMillis();
        long last = lastBackpressureViolation.getAndSet(now);

        // If violations are happening close together, increase backoff
        if (now - last < 5000) { // Within 5 seconds
            int violations = consecutiveBackpressureViolations.incrementAndGet();
            if (violations > 1) {
                // Exponential backoff up to a limit
                int newBackoff = Math.min(10, currentBackoff.get() * 2);
                currentBackoff.set(newBackoff);

                // Reduce our effective window size
                int currentWindow = serverWindowSize.get();
                int newWindow = Math.max(20, currentWindow / 2);
                serverWindowSize.set(newWindow);

                logger.warn("Received multiple backpressure warnings. Reducing window to {} and setting backoff to {}x",
                        newWindow, newBackoff);
            }
        } else {
            // Reset if it's been a while
            consecutiveBackpressureViolations.set(1);
        }
    }

    @Override
    public void close() {
        LOG.debug("Closing client...");
        isShutdown.set(true);
        stopPingPong();
        try {
            if (isStreamActive && !latch.await(10, TimeUnit.SECONDS)) {
                LOG.warn("Timed out waiting for stream completion");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Interrupted while waiting for stream completion");
        }
        if (requestObserver != null && !streamCompleted.get()) {
            try {
                if (requestObserver instanceof ClientCallStreamObserver) {
                    ClientCallStreamObserver<?> clientObserver = (ClientCallStreamObserver<?>) requestObserver;
                    if (clientObserver.isReady()) {
                        streamCompleted.set(true);
                        requestObserver.onCompleted();
                        LOG.debug("Request observer completed");
                    } else {
                        LOG.debug("Request observer not ready, skipping onCompleted");
                    }
                } else {
                    LOG.debug("Request observer is not a ClientCallStreamObserver, skipping onCompleted");
                }
            } catch (Exception e) {
                LOG.debug("Exception during request observer completion, likely already closed: {}", e.getMessage());
            }
        }

        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted during channel shutdown");
            } finally {
                processingExecutor.shutdown();
                try {
                    if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        processingExecutor.shutdownNow();
                        LOG.debug("Processing executor forcibly shut down");
                    }
                } catch (InterruptedException e) {
                    processingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                    LOG.warn("Interrupted during executor shutdown");
                }
            }
        }

        if (pingScheduler != null && !pingScheduler.isShutdown()) {
            try {
                pingScheduler.shutdown();
                if (!pingScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    pingScheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                pingScheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        LOG.info("Client closed");
    }

    private AuditRequest convertToAuditRequest(UserPrompt userPrompt, String streamId, String requestId) {
        List<StackTraceElementList> stackTraceElementLists = new ArrayList<>();
        for (List<StackTraceElement> innerList : userPrompt.getStackTrace()) {
            StackTraceElementList stackTraceElementList = StackTraceElementList.newBuilder()
                    .addAllElements(innerList.stream()
                            .map(this::convertToStackTraceElement)
                            .collect(Collectors.toList()))
                    .build();
            stackTraceElementLists.add(stackTraceElementList);
        }

        AuditRequest.Builder builder = AuditRequest.newBuilder();
        builder.setIssueData(IssueData.newBuilder()
                .setAccuracy(userPrompt.getIssueData().getAccuracy())
                .setAnalyzerName(userPrompt.getIssueData().getAnalyzerName())
                .setClassId(userPrompt.getIssueData().getClassID())
                .setConfidence(userPrompt.getIssueData().getConfidence())
                .setDefaultSeverity(userPrompt.getIssueData().getDefaultSeverity())
                .setImpact(userPrompt.getIssueData().getImpact())
                .setInstanceId(userPrompt.getIssueData().getInstanceID())
                .setInstanceSeverity(userPrompt.getIssueData().getInstanceSeverity())
                .setFiletype(userPrompt.getIssueData().getFiletype())
                .setKingdom(userPrompt.getIssueData().getKingdom())
                .setLikelihood(userPrompt.getIssueData().getLikelihood())
                .setPriority(userPrompt.getIssueData().getPriority())
                .setProbability(userPrompt.getIssueData().getProbability())
                .setSubType(userPrompt.getIssueData().getSubType())
                .setType(userPrompt.getIssueData().getType())
                .build());

        builder.setAnalysisInfo(AnalysisInfo.newBuilder()
                .setShortDescription(userPrompt.getAnalysisInfo().getShortDescription())
                .setExplanation(userPrompt.getAnalysisInfo().getExplanation())
                .build());

        builder.addAllStackTrace(stackTraceElementLists);
        builder.addAllFirstStackTrace(userPrompt.getFirstStackTrace().stream()
                .map(this::convertToStackTraceElement)
                .collect(Collectors.toList()));

        builder.addAllLongestStackTrace(userPrompt.getLongestStackTrace().stream()
                .map(this::convertToStackTraceElement)
                .collect(Collectors.toList()));

        builder.addAllFiles(userPrompt.getFiles().stream()
                .map(file -> File.newBuilder()
                        .setName(file.getName())
                        .setContent(file.getContent())
                        .setSegment(file.isSegment())
                        .setStartLine(file.getStartLine())
                        .setEndLine(file.getEndLine())
                        .build())
                .collect(Collectors.toList()));

        builder.setLastStackTraceElement(convertToStackTraceElement(userPrompt.getLastStackTraceElement()));
        builder.addAllProgrammingLanguages(userPrompt.getProgrammingLanguages());
        builder.setFileExtension(userPrompt.getFileExtension());
        builder.setLanguage(userPrompt.getLanguage());
        builder.setCategory(userPrompt.getCategory() == null ? "" : userPrompt.getCategory());
        builder.setSource(convertToStackTraceElement(userPrompt.getSource()));
        builder.setSink(convertToStackTraceElement(userPrompt.getSink()));
        builder.setCategoryLevel(userPrompt.getCategoryLevel() == null ? "" : userPrompt.getCategoryLevel());
        builder.setRequestId(requestId);
        builder.setStreamId(streamId);

        return builder.build();
    }

    private com.fortify.aviator.grpc.StackTraceElement convertToStackTraceElement(StackTraceElement element) {
        if (element == null) return null;

        return com.fortify.aviator.grpc.StackTraceElement.newBuilder()
                .setFilename(element.getFilename())
                .setLine(element.getLine())
                .setCode(element.getCode())
                .setNodeType(element.getNodeType())
                .setFragment(Fragment.newBuilder()
                        .setContent(element.getFragment().getContent())
                        .setStartLine(element.getFragment().getStartLine())
                        .setEndLine(element.getFragment().getEndLine())
                        .build())
                .setAdditionalInfo(element.getAdditionalInfo())
                .setTaintflags(element.getTaintflags() == null ? "" : element.getTaintflags())
                .addAllInnerStackTrace(element.getInnerStackTrace().stream()
                        .map(this::convertToStackTraceElement)
                        .collect(Collectors.toList()))
                .build();
    }

    private AuditResponse convertToAuditResponse(AuditorResponse response) {
        AuditResponse auditResponse = new AuditResponse();
        auditResponse.setAuditResult(new com.fortify.cli.aviator.core.model.AuditResult(
                response.getAuditResult().getTagValue(),
                response.getAuditResult().getComment()
        ));
        auditResponse.setInputToken(response.getInputToken());
        auditResponse.setOutputToken(response.getOutputToken());
        auditResponse.setStatus(response.getStatus());
        auditResponse.setStatusMessage(response.getStatusMessage());
        auditResponse.setIssueId(response.getIssueId());
        auditResponse.setTier(response.getTier());
        auditResponse.setAviatorPredictionTag(response.getAviatorPredictionTag());
        auditResponse.setIsAviatorProcessed(response.getIsAviatorProcessed());
        auditResponse.setUserPrompt(response.getUserPrompt());
        auditResponse.setSystemPrompt(response.getSystemPrompt());
        return auditResponse;
    }

    @FunctionalInterface
    interface GrpcCall<S, T, R> {
        R call(S stub, T request) throws StatusRuntimeException;
    }

    private <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation) {
        try {
            S stubWithDeadline = stub.withDeadlineAfter(defaultTimeoutSeconds, TimeUnit.SECONDS);
            return call.call(stubWithDeadline, request);
        } catch (StatusRuntimeException e) {
            Status status = e.getStatus();
            String description = status.getDescription() != null ? status.getDescription() : "Unknown gRPC error from server";

            switch (status.getCode()) {
                case INVALID_ARGUMENT:
                case NOT_FOUND:
                case ALREADY_EXISTS:
                case FAILED_PRECONDITION:
                case OUT_OF_RANGE:
                    String simpleMsg = String.format("Error during %s: %s", operation, description);
                    throw new AviatorSimpleException(simpleMsg);
                case PERMISSION_DENIED:
                    if (description.contains("Invalid signature")) {
                        throw new AviatorSimpleException(
                                "Invalid signature. Please verify the private key configured for FCLI matches the public key registered for your user on the Aviator server for the current tenant.");
                    } else {
                        String permMsg = String.format("Permission denied during %s: %s", operation, description);
                        throw new AviatorSimpleException(permMsg);
                    }
                case UNAUTHENTICATED:
                    throw new AviatorSimpleException("Authentication failed. Please check your credentials.");
                case DEADLINE_EXCEEDED:
                    throw new AviatorSimpleException("The operation took too long to complete. Please try again later.");
                case RESOURCE_EXHAUSTED:
                    throw new AviatorSimpleException("Resource limits have been exceeded. Please try again later or contact support.");
                case CANCELLED:
                case UNKNOWN:
                case ABORTED:
                case UNIMPLEMENTED:
                case INTERNAL:
                case UNAVAILABLE:
                case DATA_LOSS:
                default:
                    String techMessage = String.format("gRPC call for %s failed: %s (Status: %s)", operation, description, status.getCode());
                    LOG.debug(techMessage, e);
                    throw new AviatorTechnicalException(techMessage, e);
            }
        } catch (Exception e) {
            String errorMessage = "Unexpected error during " + operation + ": " + e.getMessage();
            LOG.error(errorMessage, e);
            throw new AviatorTechnicalException(errorMessage, e);
        }
    }

    public Application createApplication(String name, String tenantName, String signature, String message) {
        CreateApplicationRequest request = CreateApplicationRequest.newBuilder()
                .setName(name)
                .setTenantName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::createApplication, request, Constants.OP_CREATE_APP);
    }

    public Application updateApplication(String projectId, String newName, String signature, String message, String tenantName) {
        UpdateApplicationRequest request = UpdateApplicationRequest.newBuilder()
                .setId(Long.parseLong(projectId))
                .setName(newName)
                .setTenantName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::updateApplication, request, Constants.OP_UPDATE_APP);
    }

    public ApplicationResponseMessage deleteApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder()
                .setId(Long.parseLong(projectId))
                .setSignature(signature)
                .setMessage(message)
                .setTenantName(tenantName)
                .build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::deleteApplication, request, Constants.OP_DELETE_APP);
    }

    public Application getApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder()
                .setId(Long.parseLong(projectId))
                .setSignature(signature)
                .setMessage(message)
                .setTenantName(tenantName)
                .build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplication, request, Constants.OP_GET_APP);
    }

    public List<Application> listApplication(String tenantName, String signature, String message) {
        ApplicationByTenantName request = ApplicationByTenantName.newBuilder()
                .setName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        ApplicationList applicationList = executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::listApplications, request, Constants.OP_LIST_APPS);
        return applicationList.getApplicationsList();
    }

    public TokenGenerationResponse generateToken(String email, String tokenName, String signature, String message, String tenantName, String endDate) {
        TokenGenerationRequest request = TokenGenerationRequest.newBuilder()
                .setEmail(email != null ? email : "")
                .setCustomTokenName(tokenName != null ? tokenName : "")
                .setRequestSignature(signature)
                .setEndDate(StringUtil.isEmpty(endDate) ? "" : endDate)
                .setMessage(message)
                .setTenantName(tenantName)
                .build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::generateToken, request, Constants.OP_GENERATE_TOKEN);
    }

    public ListTokensResponse listTokens(String email, String tenantName, String signature, String message) {
        ListTokensRequest request = ListTokensRequest.newBuilder()
                .setEmail(email)
                .setRequestSignature(signature)
                .setMessage(message)
                .setTenantName(tenantName)
                .setIgnorePagination(true)
                .build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::listTokens, request, Constants.OP_LIST_TOKENS);
    }

    public RevokeTokenResponse revokeToken(String token, String email, String tenantName, String signature, String message) {
        RevokeTokenRequest request = RevokeTokenRequest.newBuilder()
                .setToken(token)
                .setEmail(email)
                .setTenantName(tenantName)
                .setRequestSignature(signature)
                .setMessage(message)
                .build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::revokeToken, request, Constants.OP_REVOKE_TOKEN);
    }

    public DeleteTokenResponse deleteToken(String token, String email, String tenantName, String signature, String message) {
        DeleteTokenRequest request = DeleteTokenRequest.newBuilder()
                .setToken(token)
                .setEmail(email)
                .setTenantName(tenantName)
                .setRequestSignature(signature)
                .setMessage(message)
                .build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::deleteToken, request, Constants.OP_DELETE_TOKEN);
    }

    public TokenValidationResponse validateToken(String token, String tenantName, String signature, String message) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder()
                .setToken(token)
                .setTenantName(tenantName)
                .setRequestSignature(signature)
                .setMessage(message)
                .build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::validateToken, request, Constants.OP_VALIDATE_TOKEN);
    }

    public List<Entitlement> listEntitlements(String tenantName, String signature, String message) {
        ListEntitlementsByTenantRequest request = ListEntitlementsByTenantRequest.newBuilder()
                .setTenantName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        ListEntitlementsByTenantResponse response = executeGrpcCall(entitlementServiceBlockingStub, EntitlementServiceGrpc.EntitlementServiceBlockingStub::listEntitlementsByTenant, request, Constants.OP_LIST_ENTITLEMENTS);
        return response.getEntitlementsList();
    }
}