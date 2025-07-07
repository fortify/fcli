package com.fortify.cli.aviator.grpc;

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
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.Autoremediation;
import com.fortify.cli.aviator.audit.model.Change;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.StringUtil;
import com.fortify.grpc.token.DeleteTokenRequest;
import com.fortify.grpc.token.DeleteTokenResponse;
import com.fortify.grpc.token.ListTokensByDeveloperRequest;
import com.fortify.grpc.token.ListTokensRequest;
import com.fortify.grpc.token.ListTokensResponse;
import com.fortify.grpc.token.RevokeTokenRequest;
import com.fortify.grpc.token.RevokeTokenResponse;
import com.fortify.grpc.token.TokenGenerationRequest;
import com.fortify.grpc.token.TokenGenerationResponse;
import com.fortify.grpc.token.TokenServiceGrpc;
import com.fortify.grpc.token.TokenValidationRequest;
import com.fortify.grpc.token.TokenValidationResponse;
import com.fortify.grpc.token.ValidateUserTokenRequest;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
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

public class AviatorGrpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClient.class);

    private final IAviatorLogger logger;

    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    private static final int INITIAL_REQUEST_WINDOW = 100;
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 500;
    private static final long MAX_DELAY_MS = 5000;

    private static class RequestWrapper {
        final UserPrompt userPrompt;
        int attemptCount = 0;

        RequestWrapper(UserPrompt userPrompt) {
            this.userPrompt = userPrompt;
        }
    }

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

    private final Map<String, RequestMetrics> requestMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, RequestWrapper> inflightRequests = new ConcurrentHashMap<>();

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

    private final Deque<RequestWrapper> processingQueue = new ConcurrentLinkedDeque<>();


    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        LOG.info("Initializing AviatorGrpcClient with ManagedChannel");
        this.logger = logger;
        this.streamId = UUID.randomUUID().toString();
        this.channel = channel;

        this.asyncStub = AuditorServiceGrpc.newStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();

        this.blockingStub = ApplicationServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();

        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();

        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();

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
        this(ManagedChannelBuilder.forAddress(host, port).useTransportSecurity().maxInboundMessageSize(MAX_MESSAGE_SIZE).keepAliveTime(30, TimeUnit.SECONDS).keepAliveTimeout(10, TimeUnit.SECONDS).keepAliveWithoutCalls(true).enableRetry().compressorRegistry(CompressorRegistry.getDefaultInstance()).decompressorRegistry(DecompressorRegistry.getDefaultInstance()).build(), defaultTimeoutSeconds, logger, pingIntervalSeconds);
        LOG.info("Initialized AviatorGrpcClient - Host: {}, Port: {}", host, port);
    }

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger) {
        this(channel, defaultTimeoutSeconds, logger, 30);
    }

    private void startPingPong() {
        if (isPinging.compareAndSet(false, true)) {
            logger.info("Starting ping-pong keepalive with interval of {} seconds", pingIntervalSeconds);
            pingTask = pingScheduler.scheduleAtFixedRate(this::sendPing, pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS);
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
                PingRequest pingRequest = PingRequest.newBuilder().setStreamId(streamId).setTimestamp(System.currentTimeMillis()).build();

                UserPromptRequest pingMsg = UserPromptRequest.newBuilder().setPing(pingRequest).build();

                LOG.info("Sending ping streamId: {}", streamId);
                requestObserver.onNext(pingMsg);
            }
        } catch (Exception e) {
            if (!streamCompleted.get()) {
                LOG.warn("Failed to send ping: {}", e.getMessage());
            }
        }
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(Queue<UserPrompt> requests, String projectName, String FPRBuildId, String SSCApplicationName, String SSCApplicationVersion, String token) {
        isStreamActive = true;
        if (requests == null || requests.isEmpty()) {
            LOG.info("No issues to process");
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        requests.stream().map(RequestWrapper::new).forEach(this.processingQueue::add);

        final int totalRequests = processingQueue.size();
        logger.info("Starting processing - Total Issues: " + totalRequests);
        final CompletableFuture<Map<String, AuditResponse>> resultFuture = new CompletableFuture<>();
        final Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        final AtomicInteger processedRequests = new AtomicInteger(0);

        ClientResponseObserver<UserPromptRequest, AuditorResponse> responseObserver = new ClientResponseObserver<>() {
            private final AtomicBoolean isInitialized = new AtomicBoolean(false);

            @Override
            public void beforeStart(ClientCallStreamObserver<UserPromptRequest> requestStream) {
                requestObserver = requestStream;
            }

            @Override
            public void onNext(AuditorResponse response) {
                logger.info("Received response - Status: " + response.getStatus() + ", RequestId: " + response.getRequestId());
                if ("PONG".equals(response.getStatus())) {
                    logger.info("Received pong from server: StreamId: {}, Client timestamp: {}, Server timestamp: {}, RequestId: {}", response.getStreamId(), response.getPong().getClientTimestamp(), response.getPong().getServerTimestamp(), response.getRequestId());
                    return;
                }

                if ("SERVER_BUSY".equals(response.getStatus())) {
                    handleServerBusy(response.getRequestId(), totalRequests, processedRequests, responses, resultFuture);
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

                RequestWrapper completedWrapper = inflightRequests.remove(response.getRequestId());
                if (completedWrapper == null) {
                    if (!isInitialized.get()) {
                        if ("SUCCESS".equals(response.getStatus())) {
                            isInitialized.set(true);
                            initLatch.countDown();
                            logger.info("Stream initialized successfully");
                            startPingPong();
                        } else {
                            String errorMessage = "Stream initialization failed: " + response.getStatusMessage();
                            if (!resultFuture.isDone()) {
                                resultFuture.completeExceptionally(new AviatorTechnicalException(errorMessage));
                            }
                            if (requestObserver != null) {
                                requestObserver.onCompleted();
                            }
                            streamCompleted.set(true);
                            latch.countDown();
                        }
                    } else {
                        LOG.debug("Received response for an unknown or already processed requestId: {}", response.getRequestId());
                    }
                    return;
                }

                RequestMetrics metrics = requestMetricsMap.remove(response.getRequestId());
                if (metrics != null) {
                    metrics.complete(response.getStatus());
                    logger.info("Request {} ({}) completed with status {} in {}ms", response.getRequestId(), completedWrapper.userPrompt.getIssueData().getInstanceID(), response.getStatus(), metrics.getDuration());
                }

                outstandingRequests.decrementAndGet();
                requestSemaphore.release();

                AuditResponse auditResponse = convertToAuditResponse(response);
                responses.put(completedWrapper.userPrompt.getIssueData().getInstanceID(), auditResponse);
                int completed = processedRequests.incrementAndGet();

                logger.progress("Processed " + completed + " out of " + totalRequests + " issues");

                if (completed >= totalRequests) {
                    logger.info("All requests accounted for, completing stream.");
                    if (streamCompleted.compareAndSet(false, true) && requestObserver != null) {
                        requestObserver.onCompleted();
                    }
                    if (!resultFuture.isDone()) {
                        resultFuture.complete(responses);
                    }
                    latch.countDown();
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
                logger.progress("Stream completed by server");
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
            UserPromptRequest initRequest = UserPromptRequest.newBuilder().setInit(StreamInitRequest.newBuilder().setStreamId(streamId).setRequestId(initRequestId).setToken(token).setApplicationName(projectName).setSscApplicationName(SSCApplicationName).setSscApplicationVersion(SSCApplicationVersion).setFprBuildId(FPRBuildId).setTotalReportedIssues(totalRequests).setTotalIssuesToPredict(totalRequests).build()).build();

            requestObserver.onNext(initRequest);
            LOG.info("Client Id  for stream initialization {}", streamId);

            processingExecutor.submit(() -> {
                try {
                    if (!initLatch.await(30, TimeUnit.SECONDS)) {
                        throw new AviatorTechnicalException("Stream initialization timed out");
                    }
                    processRequestQueue(totalRequests, processedRequests, responses, resultFuture);
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

    private void processRequestQueue(int totalRequests, AtomicInteger processedRequests, Map<String, AuditResponse> responses, CompletableFuture<Map<String, AuditResponse>> resultFuture) {
        logger.progress("Starting to process issues...");

        long startTime = System.currentTimeMillis();
        long maxProcessingTimeMs = defaultTimeoutSeconds * 1000 * 2; // Give extra time for retries

        while (!isShutdown.get() && !streamCompleted.get()) {
            try {

                if (processingQueue.isEmpty()) {
                    if (processedRequests.get() >= totalRequests) {
                        logger.info("All requests processed successfully. Exiting processing loop.");
                        break;
                    }

                    if (outstandingRequests.get() == 0) {
                        logger.warn("No outstanding requests and queue is empty, but processed count ({}) is less than total ({}). " + "Some requests may have been permanently failed.", processedRequests.get(), totalRequests);
                        break;
                    }

                    try {
                        Thread.sleep(100);
                        continue;
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AviatorTechnicalException("Thread interrupted while waiting for retry queue", ie);
                    }
                }

                requestSemaphore.acquire();

                if (streamCompleted.get()) {
                    requestSemaphore.release();
                    break;
                }

                RequestWrapper wrapper = processingQueue.poll();
                if (wrapper == null) {
                    requestSemaphore.release();
                    continue;
                }
                if (wrapper.attemptCount == 0) {
                    outstandingRequests.incrementAndGet();
                }
                if (wrapper.attemptCount > 0) {
                    long delay = (long) (BASE_DELAY_MS * Math.pow(2, wrapper.attemptCount - 1));
                    delay = Math.min(delay, MAX_DELAY_MS) + ThreadLocalRandom.current().nextLong(100);
                    logger.warn("Applying retry delay of {}ms for instance {} (attempt {}/{})", delay, wrapper.userPrompt.getIssueData().getInstanceID(), wrapper.attemptCount + 1, MAX_RETRIES);
                    Thread.sleep(delay);
                }

                submitUserPrompt(wrapper);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new AviatorTechnicalException("Thread interrupted while processing queue", ie);
            } catch (Exception e) {
                if (!streamCompleted.get()) {
                    LOG.error("Error in processing loop: {}", e.getMessage(), e);
                    throw new AviatorTechnicalException("Error in processing loop", e);
                }
            }
        }

        logger.info("Processing queue loop completed. Queue size: {}, Processed: {}/{}, Outstanding: {}", processingQueue.size(), processedRequests.get(), totalRequests, outstandingRequests.get());
    }

    private void submitUserPrompt(RequestWrapper wrapper) {
        String requestId = UUID.randomUUID().toString();
        inflightRequests.put(requestId, wrapper);

        AuditRequest auditRequest = convertToAuditRequest(wrapper.userPrompt, streamId, requestId);
        UserPromptRequest promptRequest = UserPromptRequest.newBuilder().setAudit(auditRequest).build();

        requestMetricsMap.put(requestId, new RequestMetrics());

        boolean sent = sendRequestWithRetry(promptRequest, MAX_RETRIES);
        if (!sent) {
            LOG.error("Failed to send request for instance {} after all retries. The request will be dropped.", wrapper.userPrompt.getIssueData().getInstanceID());
            inflightRequests.remove(requestId);
            requestMetricsMap.remove(requestId);
            outstandingRequests.decrementAndGet();
            requestSemaphore.release();
        }
    }

    private void handleServerBusy(String requestId, int totalRequests, AtomicInteger processedRequests, Map<String, AuditResponse> responses, CompletableFuture<Map<String, AuditResponse>> resultFuture) {
        RequestWrapper wrapperToRetry = inflightRequests.remove(requestId);
        if (wrapperToRetry == null) {
            LOG.warn("Received SERVER_BUSY for unknown or already completed requestId: {}", requestId);
            return;
        }

        requestMetricsMap.remove(requestId);
        requestSemaphore.release();

        wrapperToRetry.attemptCount++;

        if (wrapperToRetry.attemptCount > MAX_RETRIES) {
            LOG.error("Request for instance {} failed after {} retries due to server being busy. Dropping request.", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), MAX_RETRIES);

            AuditResponse failedResponse = new AuditResponse();
            failedResponse.setIssueId(wrapperToRetry.userPrompt.getIssueData().getInstanceID());
            failedResponse.setStatus("RETRY_LIMIT_EXCEEDED");
            failedResponse.setStatusMessage("Request failed after " + MAX_RETRIES + " retries due to server overload.");
            responses.put(wrapperToRetry.userPrompt.getIssueData().getInstanceID(), failedResponse);
            int completed = processedRequests.incrementAndGet();

            logger.progress("Request permanently failed due to server busy - Processed " + completed + " out of " + totalRequests + " issues");
            int stillOutstanding = outstandingRequests.decrementAndGet();
            LOG.warn("Request for instance {} permanently failed. Remaining outstanding requests: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), stillOutstanding);
            if (completed >= totalRequests) {
                logger.info("All requests accounted for after permanent failure, completing stream.");
                if (streamCompleted.compareAndSet(false, true) && requestObserver != null) {
                    requestObserver.onCompleted();
                }
                if (!resultFuture.isDone()) resultFuture.complete(responses);
                latch.countDown();
            }
        } else {
            logger.warn("Server is busy for instanceId {}. Re-queueing for retry (Attempt {}/{}). Queue size before re-queue: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), wrapperToRetry.attemptCount, MAX_RETRIES, processingQueue.size());

            processingQueue.addFirst(wrapperToRetry);

            logger.info("Request re-queued. Queue size after re-queue: {}", processingQueue.size());
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
                throw e;
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

                logger.warn("Received multiple backpressure warnings. Reducing window to {} and setting backoff to {}x", newWindow, newBackoff);
            }
        } else {
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
        if (userPrompt.getStackTrace() != null) {
            for (List<StackTraceElement> innerList : userPrompt.getStackTrace()) {
                StackTraceElementList stackTraceElementList = StackTraceElementList.newBuilder().addAllElements(innerList.stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).build();
                stackTraceElementLists.add(stackTraceElementList);
            }
        }

        AuditRequest.Builder builder = AuditRequest.newBuilder();
        if (userPrompt.getIssueData() != null) {
            builder.setIssueData(IssueData.newBuilder().setAccuracy(userPrompt.getIssueData().getAccuracy()).setAnalyzerName(userPrompt.getIssueData().getAnalyzerName() == null ? "" : userPrompt.getIssueData().getAnalyzerName()).setClassId(userPrompt.getIssueData().getClassID() == null ? "" : userPrompt.getIssueData().getClassID()).setConfidence(userPrompt.getIssueData().getConfidence()).setDefaultSeverity(userPrompt.getIssueData().getDefaultSeverity() == null ? "" : userPrompt.getIssueData().getDefaultSeverity()).setImpact(userPrompt.getIssueData().getImpact()).setInstanceId(userPrompt.getIssueData().getInstanceID() == null ? "" : userPrompt.getIssueData().getInstanceID()).setInstanceSeverity(userPrompt.getIssueData().getInstanceSeverity() == null ? "" : userPrompt.getIssueData().getInstanceSeverity()).setFiletype(userPrompt.getIssueData().getFiletype() == null ? "" : userPrompt.getIssueData().getFiletype()).setKingdom(userPrompt.getIssueData().getKingdom() == null ? "" : userPrompt.getIssueData().getKingdom()).setLikelihood(userPrompt.getIssueData().getLikelihood()).setPriority(userPrompt.getIssueData().getPriority() == null ? "" : userPrompt.getIssueData().getPriority()).setProbability(userPrompt.getIssueData().getProbability()).setSubType(userPrompt.getIssueData().getSubType() == null ? "" : userPrompt.getIssueData().getSubType()).setType(userPrompt.getIssueData().getType() == null ? "" : userPrompt.getIssueData().getType()).build());
        }

        if (userPrompt.getAnalysisInfo() != null) {
            builder.setAnalysisInfo(AnalysisInfo.newBuilder().setShortDescription(userPrompt.getAnalysisInfo().getShortDescription() == null ? "" : userPrompt.getAnalysisInfo().getShortDescription()).setExplanation(userPrompt.getAnalysisInfo().getExplanation() == null ? "" : userPrompt.getAnalysisInfo().getExplanation()).build());
        }

        builder.addAllStackTrace(stackTraceElementLists);
        if (userPrompt.getFirstStackTrace() != null) {
            builder.addAllFirstStackTrace(userPrompt.getFirstStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList()));
        }
        if (userPrompt.getLongestStackTrace() != null) {
            builder.addAllLongestStackTrace(userPrompt.getLongestStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList()));
        }
        if (userPrompt.getFiles() != null) {
            builder.addAllFiles(userPrompt.getFiles().stream().map(file -> File.newBuilder().setName(file.getName() == null ? "" : file.getName()).setContent(file.getContent() == null ? "" : file.getContent()).setSegment(file.isSegment()).setStartLine(file.getStartLine()).setEndLine(file.getEndLine()).build()).collect(Collectors.toList()));
        }
        if (userPrompt.getLastStackTraceElement() != null) {
            builder.setLastStackTraceElement(convertToStackTraceElement(userPrompt.getLastStackTraceElement()));
        }
        if (userPrompt.getProgrammingLanguages() != null) {
            builder.addAllProgrammingLanguages(userPrompt.getProgrammingLanguages());
        }
        builder.setFileExtension(userPrompt.getFileExtension() == null ? "" : userPrompt.getFileExtension());
        builder.setLanguage(userPrompt.getLanguage() == null ? "" : userPrompt.getLanguage());
        builder.setCategory(userPrompt.getCategory() == null ? "" : userPrompt.getCategory());
        if (userPrompt.getSource() != null) {
            builder.setSource(convertToStackTraceElement(userPrompt.getSource()));
        }
        if (userPrompt.getSink() != null) {
            builder.setSink(convertToStackTraceElement(userPrompt.getSink()));
        }
        builder.setCategoryLevel(userPrompt.getCategoryLevel() == null ? "" : userPrompt.getCategoryLevel());
        builder.setRequestId(requestId);
        builder.setStreamId(streamId);

        return builder.build();
    }

    private com.fortify.aviator.grpc.StackTraceElement convertToStackTraceElement(StackTraceElement element) {
        if (element == null) return com.fortify.aviator.grpc.StackTraceElement.getDefaultInstance();

        com.fortify.aviator.grpc.StackTraceElement.Builder builder = com.fortify.aviator.grpc.StackTraceElement.newBuilder();
        if (element.getFilename() != null) builder.setFilename(element.getFilename());
        builder.setLine(element.getLine());
        if (element.getCode() != null) builder.setCode(element.getCode());
        if (element.getNodeType() != null) builder.setNodeType(element.getNodeType());
        if (element.getFragment() != null) {
            builder.setFragment(Fragment.newBuilder().setContent(element.getFragment().getContent() == null ? "" : element.getFragment().getContent()).setStartLine(element.getFragment().getStartLine()).setEndLine(element.getFragment().getEndLine()).build());
        }
        if (element.getAdditionalInfo() != null) builder.setAdditionalInfo(element.getAdditionalInfo());
        if (element.getTaintflags() != null) builder.setTaintflags(element.getTaintflags());
        if (element.getInnerStackTrace() != null) {
            builder.addAllInnerStackTrace(element.getInnerStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList()));
        }
        return builder.build();
    }

    private AuditResponse convertToAuditResponse(AuditorResponse response) {
        AuditResponse auditResponse = new AuditResponse();
        com.fortify.cli.aviator.audit.model.AuditResult.AuditResultBuilder auditResultBuilder = com.fortify.cli.aviator.audit.model.AuditResult.builder().tagValue(response.getAuditResult().getTagValue()).comment(response.getAuditResult().getComment());

        if (response.getAuditResult().hasAutoremediation()) {
            com.fortify.aviator.grpc.Autoremediation grpcAutoremediation = response.getAuditResult().getAutoremediation();
            List<Change> cliChanges = grpcAutoremediation.getChangesList().stream().map(grpcChange -> Change.builder().file(grpcChange.getFile()).fromLine(grpcChange.getFromLine()).toLine(grpcChange.getToLine()).replaceWith(grpcChange.getReplaceWith()).build()).collect(Collectors.toList());
            auditResultBuilder.autoremediation(Autoremediation.builder().changes(cliChanges).build());
        }
        auditResponse.setAuditResult(auditResultBuilder.build());
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
            String serverDescription = status.getDescription() != null && !status.getDescription().isBlank()
                    ? status.getDescription()
                    : "No additional details were provided by the server.";

            switch (status.getCode()) {
                case INVALID_ARGUMENT:
                    throw new AviatorSimpleException(String.format("Invalid input for %s. The server reported: %s. Please check the provided arguments.", operation, serverDescription));
                case NOT_FOUND:
                    throw new AviatorSimpleException(String.format("The requested resource was not found during %s. The server reported: %s. Please verify the name or ID is correct.", operation, serverDescription));
                case ALREADY_EXISTS:
                    throw new AviatorSimpleException(String.format("Cannot perform %s because a resource with the same identifier already exists. The server reported: %s.", operation, serverDescription));
                case FAILED_PRECONDITION:
                    throw new AviatorSimpleException(String.format("The %s operation could not be completed because a required condition was not met. The server reported: %s.", operation, serverDescription));

                case PERMISSION_DENIED:
                    if (serverDescription.toLowerCase().contains("invalid signature")) {
                        throw new AviatorSimpleException("Permission Denied: Invalid signature. Please verify the private key in your admin configuration is correct and corresponds to the public key registered with Aviator.");
                    } else {
                        throw new AviatorSimpleException(String.format("Permission Denied for %s. You may not have the required roles for this action. Server details: %s", operation, serverDescription));
                    }
                case UNAUTHENTICATED:
                    throw new AviatorSimpleException("Authentication Failed: The token or credentials used are invalid or expired. Please log in again using 'fcli aviator session login' or verify your admin configuration.");

                case RESOURCE_EXHAUSTED:
                    throw new AviatorSimpleException(String.format("The server's resource limits were exceeded during %s. Please try again later or contact support. Server details: %s", operation, serverDescription));
                case DEADLINE_EXCEEDED:
                    String timeoutMessage = String.format(
                            "The %s operation timed out because the server did not respond in time.\n\n" +
                                    "Please check the following:\n" +
                                    "  1. The Aviator URL in your configuration is correct and reachable.\n" +
                                    "  2. Your network connection is stable and any firewalls or proxies are properly configured.\n\n" +
                                    "If the URL and network are correct, the server may be experiencing high load. Please try again later.",
                            operation
                    );
                    throw new AviatorSimpleException(timeoutMessage);

                case CANCELLED:
                case UNKNOWN:
                case ABORTED:
                case UNIMPLEMENTED:
                case INTERNAL:
                case UNAVAILABLE:
                case DATA_LOSS:
                default:
                    String techMessage = String.format("A technical error occurred on the server while performing the %s operation. Please check the fcli logs for details or contact support if the issue persists.", operation);
                    String logMessage = String.format("gRPC call for '%s' failed with status %s (%s): %s", operation, status.getCode(), status.getCode().name(), serverDescription);
                    LOG.error(logMessage, e);
                    throw new AviatorTechnicalException(techMessage, e);
            }
        } catch (Exception e) {
            String errorMessage = "An unexpected client-side error occurred during the " + operation + " operation.";
            LOG.error(errorMessage, e);
            throw new AviatorTechnicalException(errorMessage, e);
        }
    }
    public Application createApplication(String name, String tenantName, String signature, String message) {
        CreateApplicationRequest request = CreateApplicationRequest.newBuilder().setName(name).setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::createApplication, request, Constants.OP_CREATE_APP);
    }

    public Application updateApplication(String projectId, String newName, String signature, String message, String tenantName) {
        UpdateApplicationRequest request = UpdateApplicationRequest.newBuilder().setId(Long.parseLong(projectId)).setName(newName).setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::updateApplication, request, Constants.OP_UPDATE_APP);
    }

    public ApplicationResponseMessage deleteApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::deleteApplication, request, Constants.OP_DELETE_APP);
    }

    public Application getApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplication, request, Constants.OP_GET_APP);
    }

    public List<Application> listApplication(String tenantName, String signature, String message) {
        ApplicationByTenantName request = ApplicationByTenantName.newBuilder().setName(tenantName).setSignature(signature).setMessage(message).build();
        ApplicationList applicationList = executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::listApplications, request, Constants.OP_LIST_APPS);
        return applicationList.getApplicationsList();
    }

    public TokenGenerationResponse generateToken(String email, String tokenName, String signature, String message, String tenantName, String endDate) {
        TokenGenerationRequest request = TokenGenerationRequest.newBuilder().setEmail(email != null ? email : "").setCustomTokenName(tokenName != null ? tokenName : "").setRequestSignature(signature).setEndDate(StringUtil.isEmpty(endDate) ? "" : endDate).setMessage(message).setTenantName(tenantName).build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::generateToken, request, Constants.OP_GENERATE_TOKEN);
    }

    public ListTokensResponse listTokens(String email, String tenantName, String signature, String message) {
        ListTokensRequest request = ListTokensRequest.newBuilder().setRequestSignature(signature).setMessage(message).setTenantName(tenantName).setIgnorePagination(true).build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::listTokens, request, Constants.OP_LIST_TOKENS);
    }

    public ListTokensResponse listTokensByDeveloper(String tenantName, String developerEmail, String signature, String message) {
        ListTokensByDeveloperRequest.Builder requestBuilder = ListTokensByDeveloperRequest.newBuilder().setTenantName(tenantName).setRequestSignature(signature).setMessage(message).setIgnorePagination(true);
        if (developerEmail != null) {
            requestBuilder.setDeveloperEmail(developerEmail);
        }
        ListTokensByDeveloperRequest request = requestBuilder.build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::listTokensByDeveloper, request, Constants.OP_LIST_TOKENS_BY_DEVELOPER);
    }

    public RevokeTokenResponse revokeToken(String token, String email, String tenantName, String signature, String message) {
        RevokeTokenRequest request = RevokeTokenRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::revokeToken, request, Constants.OP_REVOKE_TOKEN);
    }

    public DeleteTokenResponse deleteToken(String token, String email, String tenantName, String signature, String message) {
        DeleteTokenRequest request = DeleteTokenRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::deleteToken, request, Constants.OP_DELETE_TOKEN);
    }

    public TokenValidationResponse validateToken(String token, String tenantName, String signature, String message) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::validateToken, request, Constants.OP_VALIDATE_TOKEN);
    }

    public TokenValidationResponse validateUserToken(String token, String tenantName) {
        ValidateUserTokenRequest request = ValidateUserTokenRequest.newBuilder().setToken(token).setTenantName(tenantName != null ? tenantName : "").build();
        return executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::validateUserToken, request, Constants.OP_VALIDATE_USER_TOKEN);
    }

    public List<Entitlement> listEntitlements(String tenantName, String signature, String message) {
        ListEntitlementsByTenantRequest request = ListEntitlementsByTenantRequest.newBuilder().setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        ListEntitlementsByTenantResponse response = executeGrpcCall(entitlementServiceBlockingStub, EntitlementServiceGrpc.EntitlementServiceBlockingStub::listEntitlementsByTenant, request, Constants.OP_LIST_ENTITLEMENTS);
        return response.getEntitlementsList();
    }
}