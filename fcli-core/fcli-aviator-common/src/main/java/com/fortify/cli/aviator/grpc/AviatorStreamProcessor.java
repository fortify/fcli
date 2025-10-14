package com.fortify.cli.aviator.grpc;

import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.AuditRequest;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.PingRequest;
import com.fortify.aviator.grpc.StreamInitRequest;
import com.fortify.aviator.grpc.UserPromptRequest;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.ClientCallStreamObserver;
import io.grpc.stub.ClientResponseObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

class AviatorStreamProcessor implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorStreamProcessor.class);

    private final AviatorGrpcClient client;
    private final IAviatorLogger logger;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final java.util.concurrent.ExecutorService processingExecutor;
    private final java.util.concurrent.ScheduledExecutorService pingScheduler;
    private final long pingIntervalSeconds;
    private final long defaultTimeoutSeconds;

    private final Map<String, RequestMetrics> requestMetricsMap = new ConcurrentHashMap<>();
    private final Map<String, RequestWrapper> inflightRequests = new ConcurrentHashMap<>();

    private final AtomicInteger consecutiveBackpressureViolations = new AtomicInteger(0);
    private final AtomicLong lastBackpressureViolation = new AtomicLong(0);
    private final AtomicInteger currentBackoff = new AtomicInteger(1);
    private final AtomicInteger serverWindowSize = new AtomicInteger(Constants.INITIAL_REQUEST_WINDOW);

    private final Semaphore requestSemaphore = new Semaphore(Constants.INITIAL_REQUEST_WINDOW);
    private final AtomicInteger outstandingRequests = new AtomicInteger(0);
    private RequestHandler<UserPromptRequest> requestHandler;

    private java.util.concurrent.ScheduledFuture<?> pingTask;
    private final AtomicBoolean isPinging = new AtomicBoolean(false);

    private final java.util.concurrent.ConcurrentLinkedDeque<RequestWrapper> processingQueue = new ConcurrentLinkedDeque<>();
    private volatile StreamState currentStreamState;
    private final AtomicInteger stagnantRetryCount = new AtomicInteger(0);
    private final AtomicInteger lastProcessed = new AtomicInteger(0);
    private CountDownLatch streamLatch;
    private volatile Future<?> processingTask;
    private final Object retryLock = new Object();

    public AviatorStreamProcessor(AviatorGrpcClient client, IAviatorLogger logger, AuditorServiceGrpc.AuditorServiceStub asyncStub, java.util.concurrent.ExecutorService processingExecutor, java.util.concurrent.ScheduledExecutorService pingScheduler, long pingIntervalSeconds, long defaultTimeoutSeconds) {
        this.client = client;
        this.logger = logger;
        this.asyncStub = asyncStub;
        this.processingExecutor = processingExecutor;
        this.pingScheduler = pingScheduler;
        this.pingIntervalSeconds = pingIntervalSeconds;
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(Queue<UserPrompt> requests, String projectName, String FPRBuildId, String SSCApplicationName, String SSCApplicationVersion, String token) {
        if (requests == null || requests.isEmpty()) {
            LOG.info("No issues to process");
            return CompletableFuture.completedFuture(new HashMap<>());
        }

        String streamId = UUID.randomUUID().toString();
        currentStreamState = new StreamState(streamId, projectName, FPRBuildId, SSCApplicationName, SSCApplicationVersion, token, requests.size());

        requests.stream().map(RequestWrapper::new).forEach(wrapper -> {
            this.processingQueue.add(wrapper);
            currentStreamState.pendingIssueIds.add(wrapper.userPrompt.getIssueData().getInstanceID());
        });

        final int totalRequests = processingQueue.size();
        logger.info("Starting processing - Total Issues: " + totalRequests);
        final CompletableFuture<Map<String, AuditResponse>> resultFuture = new CompletableFuture<>();
        final Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        final AtomicInteger processedRequests = new AtomicInteger(0);

        startStreamWithRetry(responses, processedRequests, resultFuture);

        return resultFuture.exceptionally(ex -> {
            stopPingPong();
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) && ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof AviatorSimpleException) throw (AviatorSimpleException) cause;
            if (cause instanceof AviatorTechnicalException) throw (AviatorTechnicalException) cause;
            throw new AviatorTechnicalException("Processing FPR failed", cause);
        });
    }

    private void startStreamWithRetry(Map<String, AuditResponse> responses, AtomicInteger processedRequests, CompletableFuture<Map<String, AuditResponse>> resultFuture) {
        synchronized (retryLock) {
            if (client.isShutdown.get() || resultFuture.isDone()) {
                return;
            }

            if (currentStreamState.streamRetryCount > 0) {
                long delay = calculateStreamRetryDelay(currentStreamState.streamRetryCount);
                boolean infinite = true; // Assume infinite for PROTOCOL_ERROR; adjust based on last error if needed
                String maxStr = infinite ? "infinite" : String.valueOf(Constants.MAX_STREAM_RETRIES);
                logger.info("Retrying stream connection (attempt {}/{}) after {} ms delay",
                        currentStreamState.streamRetryCount + 1, maxStr, delay);

                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Interrupted during stream retry delay", e));
                    return;
                }

                currentStreamState.streamId = UUID.randomUUID().toString();

                if (processingTask != null) {
                    processingTask.cancel(true);
                }

                outstandingRequests.set(0);
                requestSemaphore.drainPermits();
                requestSemaphore.release(Constants.INITIAL_REQUEST_WINDOW);
            }

            currentStreamState.streamRetryCount++;
            currentStreamState.isStreamInitialized = false;

            requestHandler = new RequestHandler<>(currentStreamState.streamId);
            this.streamLatch = new CountDownLatch(1);
            CountDownLatch initLatch = new CountDownLatch(1);

            ClientResponseObserver<UserPromptRequest, AuditorResponse> responseObserver = createResponseObserver(
                    responses, processedRequests, resultFuture, this.streamLatch, initLatch);

            try {
                asyncStub.processStream(responseObserver);

                LOG.info("Sending initialization request for stream retry {}", currentStreamState.streamRetryCount);
                sendInitRequest();

                if (currentStreamState.streamRetryCount > 1) {
                    requestSemaphore.drainPermits();
                    requestSemaphore.release(Constants.INITIAL_REQUEST_WINDOW);
                    outstandingRequests.set(0);
                }

                processingExecutor.submit(() -> {
                    try {  // Outer try to propagate to future
                        if (!initLatch.await(30, TimeUnit.SECONDS)) {
                            throw new AviatorTechnicalException("Stream initialization timed out");
                        }

                        if (currentStreamState.streamRetryCount > 1) {
                            reQueueUnprocessedRequests();
                        }

                        processRequestQueue(currentStreamState.totalRequests, processedRequests, responses, resultFuture, this.streamLatch);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        // Check if interruption is due to normal shutdown
                        if (client.isShutdown.get() || requestHandler.isCompleted()) {
                            LOG.debug("Processing task interrupted during normal shutdown");
                            return; // Exit gracefully without propagating exception
                        }
                        resultFuture.completeExceptionally(new AviatorTechnicalException("Interrupted during request processing", e));
                    } catch (Exception e) {
                        // Only propagate exceptions if the stream hasn't completed successfully
                        if (requestHandler != null && !requestHandler.isCompleted() && !resultFuture.isDone()) {
                            resultFuture.completeExceptionally(new AviatorTechnicalException("Error during request processing execution", e));
                        } else {
                            LOG.debug("Exception caught after stream completion or shutdown: {}", e.getMessage());
                        }
                    }
                });

                this.streamLatch.await();

            } catch (Exception e) {
                LOG.error("Stream failed with error: {}", e.getMessage(), e);

                if (isRetryableError(e)) {
                    boolean infinite = isInfiniteRetryError(e);
                    if (!infinite) {
                        if (processedRequests.get() == lastProcessed.get()) {
                            int newCount = stagnantRetryCount.incrementAndGet();
                            if (newCount >= 3) {
                                String msg = "No progress after multiple retries due to persistent stream errors. Aborting.";
                                logger.error(msg);
                                resultFuture.completeExceptionally(new AviatorTechnicalException(msg, e));
                                return;
                            }
                        } else {
                            stagnantRetryCount.set(0);
                        }
                    }
                    lastProcessed.set(processedRequests.get());
                    if (infinite || currentStreamState.streamRetryCount < Constants.MAX_STREAM_RETRIES) {
                        LOG.warn("WARN: Stream encountered a retryable error. Will attempt to reconnect...");
                        if (requestHandler != null) {
                            requestHandler.complete();
                        }
                        stopPingPong();

                        startStreamWithRetry(responses, processedRequests, resultFuture);
                    } else {
                        if (requestHandler != null) {
                            requestHandler.sendError(e);
                        }
                        resultFuture.completeExceptionally(new AviatorTechnicalException("Error initiating batch processing", e));
                    }
                } else {
                    if (requestHandler != null) {
                        requestHandler.sendError(e);
                    }
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Error initiating batch processing", e));
                }
            }
        }
    }

    private ClientResponseObserver<UserPromptRequest, AuditorResponse> createResponseObserver(
            Map<String, AuditResponse> responses, AtomicInteger processedRequests,
            CompletableFuture<Map<String, AuditResponse>> resultFuture,
            CountDownLatch streamLatch, CountDownLatch initLatch) {

        return new ClientResponseObserver<UserPromptRequest, AuditorResponse>() {
            private final AtomicBoolean isInitialized = new AtomicBoolean(false);

            @Override
            public void beforeStart(ClientCallStreamObserver<UserPromptRequest> requestStream) {
                requestHandler.initialize(requestStream);
            }

            @Override
            public void onNext(AuditorResponse response) {
                logger.info("Received response - Status: " + response.getStatus() + ", RequestId: " + response.getRequestId());

                if ("PONG".equals(response.getStatus())) {
                    logger.info("Received pong from server: StreamId: {}, Client timestamp: {}, Server timestamp: {}, RequestId: {}",
                            response.getStreamId(), response.getPong().getClientTimestamp(),
                            response.getPong().getServerTimestamp(), response.getRequestId());
                    return;
                }

                if ("SERVER_BUSY".equals(response.getStatus())) {
                    handleServerBusy(response.getRequestId(), currentStreamState.totalRequests,
                            processedRequests, responses, resultFuture, streamLatch);
                    return;
                }

                if ("INTERNAL_ERROR".equals(response.getStatus())) {
                    String cliMessage = "Internal server error occurred";
                    logger.error(cliMessage);
                    resultFuture.completeExceptionally(new AviatorTechnicalException(cliMessage));
                    if (requestHandler != null) {
                        requestHandler.complete();
                    }
                    streamLatch.countDown();
                    return;
                }

                if ("BACKPRESSURE_WARNING".equals(response.getStatus())) {
                    handleBackpressureWarning();
                } else if ("BACKPRESSURE_VIOLATION".equals(response.getStatus())) {
                    logger.error("Server terminated stream due to backpressure violations: {}", response.getStatusMessage());
                    if (requestHandler != null) {
                        requestHandler.complete();
                    }
                    if (!resultFuture.isDone()) {
                        resultFuture.completeExceptionally(new AviatorTechnicalException("Stream terminated by server: " + response.getStatusMessage()));
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
                            isInitialized.set(true);
                            currentStreamState.isStreamInitialized = true;
                            initLatch.countDown();
                            logger.info("Stream initialized successfully");
                            startPingPong();
                        } else {
                            String errorMessage = "Stream initialization failed: " + response.getStatusMessage();
                            if (!resultFuture.isDone()) {
                                resultFuture.completeExceptionally(new AviatorTechnicalException(errorMessage));
                            }
                            if (requestHandler != null) {
                                requestHandler.complete();
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

                outstandingRequests.decrementAndGet();
                requestSemaphore.release();

                AuditResponse auditResponse = GrpcUtil.convertToAuditResponse(response);
                responses.put(instanceId, auditResponse);
                int completed = processedRequests.incrementAndGet();

                logger.progress("Processed " + completed + " out of " + currentStreamState.totalRequests + " issues");

                if (completed >= currentStreamState.totalRequests) {
                    logger.info("All requests accounted for, completing stream.");
                    if (requestHandler != null && !requestHandler.isCompleted()) {
                        requestHandler.complete();
                    }
                    if (!resultFuture.isDone()) {
                        resultFuture.complete(responses);
                    }
                    streamLatch.countDown();
                }
            }

            @Override
            public void onError(Throwable t) {
                stopPingPong();

                if (isRetryableError(t)) {
                    boolean infinite = isInfiniteRetryError(t);
                    if (!infinite) {
                        if (processedRequests.get() == lastProcessed.get()) {
                            int newCount = stagnantRetryCount.incrementAndGet();
                            if (newCount >= 3) {
                                String msg = "No progress after multiple retries due to persistent stream errors. Aborting.";
                                logger.error(msg);
                                if (!resultFuture.isDone()) {
                                    resultFuture.completeExceptionally(new AviatorTechnicalException(msg, t));
                                }
                                streamLatch.countDown();
                                return;
                            }
                        } else {
                            stagnantRetryCount.set(0);
                        }
                    }
                    lastProcessed.set(processedRequests.get());

                    if (infinite || currentStreamState.streamRetryCount < Constants.MAX_STREAM_RETRIES) {
                        LOG.debug("Stream encountered retryable error: {}. Will retry...", t.getMessage());
                        int reAdded = inflightRequests.size();
                        inflightRequests.values().forEach(processingQueue::addFirst);
                        inflightRequests.clear();
                        outstandingRequests.addAndGet(-reAdded);
                        requestSemaphore.release(reAdded);
                        if (outstandingRequests.get() < 0) {
                            outstandingRequests.set(0);
                        }
                        if (requestHandler != null) {
                            requestHandler.complete();
                        }
                        if (processingTask != null) {
                            processingTask.cancel(true);
                        }

                        if (!processingExecutor.isShutdown()) {
                            processingExecutor.submit(() -> startStreamWithRetry(responses, processedRequests, resultFuture));
                        } else {
                            LOG.warn("Cannot retry stream, as the processing executor is already shut down.");
                            if (!resultFuture.isDone()) {
                                resultFuture.completeExceptionally(new AviatorTechnicalException("Cannot retry stream, executor is shut down.", t));
                            }
                        }
                    } else {
                        if (!resultFuture.isDone()) {
                            LOG.error("Stream error occurred: {}", t.getMessage(), t);
                            if (t instanceof StatusRuntimeException sre) {
                                String description = sre.getStatus().getDescription() != null ?
                                        sre.getStatus().getDescription() : "Unknown gRPC error";
                                String techMessage = String.format("gRPC stream failed: %s (Status: %s)",
                                        description, sre.getStatus().getCode());
                                resultFuture.completeExceptionally(new AviatorTechnicalException(techMessage, t));
                            } else {
                                resultFuture.completeExceptionally(new AviatorTechnicalException("Stream error", t));
                            }
                        }
                    }
                    streamLatch.countDown();
                } else {
                    LOG.error("Stream error occurred: {}", t.getMessage(), t);
                    if (!resultFuture.isDone()) {
                        if (t instanceof StatusRuntimeException sre) {
                            String description = sre.getStatus().getDescription() != null ?
                                    sre.getStatus().getDescription() : "Unknown gRPC error";
                            String techMessage = String.format("gRPC stream failed: %s (Status: %s)",
                                    description, sre.getStatus().getCode());
                            resultFuture.completeExceptionally(new AviatorTechnicalException(techMessage, t));
                        } else {
                            resultFuture.completeExceptionally(new AviatorTechnicalException("Stream error", t));
                        }
                    }
                    streamLatch.countDown();
                }
            }

            @Override
            public void onCompleted() {
                stopPingPong();
                logger.progress("Stream completed by server");
                if (!resultFuture.isDone()) {
                    resultFuture.complete(responses);
                }
                streamLatch.countDown();
            }
        };
    }

    private void sendInitRequest() throws Exception {
        String initRequestId = UUID.randomUUID().toString();
        UserPromptRequest initRequest = UserPromptRequest.newBuilder()
                .setInit(StreamInitRequest.newBuilder()
                        .setStreamId(currentStreamState.streamId)
                        .setRequestId(initRequestId)
                        .setToken(currentStreamState.token)
                        .setApplicationName(currentStreamState.projectName)
                        .setSscApplicationName(currentStreamState.SSCApplicationName)
                        .setSscApplicationVersion(currentStreamState.SSCApplicationVersion)
                        .setFprBuildId(currentStreamState.FPRBuildId)
                        .setTotalReportedIssues(currentStreamState.totalRequests)
                        .setTotalIssuesToPredict(currentStreamState.totalRequests)
                        .build())
                .build();

        requestHandler.sendRequest(initRequest);
        LOG.info("Client Id for stream initialization {}", currentStreamState.streamId);
    }

    private void reQueueUnprocessedRequests() {
        Set<String> unprocessedIds = new HashSet<>(currentStreamState.pendingIssueIds);
        unprocessedIds.removeAll(currentStreamState.processedIssueIds);

        if (!unprocessedIds.isEmpty()) {
            logger.info("Re-queueing {} unprocessed requests after stream reconnection", unprocessedIds.size());

            List<String> toRemove = new ArrayList<>();
            int inFlightCount = 0;
            for (Map.Entry<String, RequestWrapper> entry : inflightRequests.entrySet()) {
                if (unprocessedIds.contains(entry.getValue().userPrompt.getIssueData().getInstanceID())) {
                    toRemove.add(entry.getKey());
                    inFlightCount++;
                }
            }
            for (String key : toRemove) {
                inflightRequests.remove(key);
            }

            outstandingRequests.addAndGet(-inFlightCount);

            requestSemaphore.release(inFlightCount);

            if (outstandingRequests.get() < 0) {
                outstandingRequests.set(0);
            }
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

    private boolean isInfiniteRetryError(Throwable t) {
        if (t instanceof StatusRuntimeException sre) {
            String description = sre.getStatus().getDescription();
            return sre.getStatus().getCode() == Status.Code.INTERNAL && description != null &&
                    description.contains("PROTOCOL_ERROR");
        }
        return false;
    }

    private long calculateStreamRetryDelay(int retryCount) {
        long delay = (long) (Constants.STREAM_RETRY_BASE_DELAY_MS * Math.pow(2, retryCount - 1));
        delay = Math.min(delay, Constants.STREAM_RETRY_MAX_DELAY_MS);
        delay += ThreadLocalRandom.current().nextLong(1000);
        return delay;
    }

    private void processRequestQueue(int totalRequests, AtomicInteger processedRequests,
                                     Map<String, AuditResponse> responses, CompletableFuture<Map<String, AuditResponse>> resultFuture,
                                     CountDownLatch streamLatch) {
        logger.progress("Starting to process issues...");

        LOG.info("Entering processRequestQueue loop, queue size: " + processingQueue.size() + ", permits: " + requestSemaphore.availablePermits() + ", outstanding: " + outstandingRequests.get());

        while (!client.isShutdown.get() && !requestHandler.isCompleted() && !resultFuture.isDone()) {
            RequestWrapper wrapper = null;
            try {
                if (outstandingRequests.get() < 0) {
                    outstandingRequests.set(0);
                }

                if (processingQueue.isEmpty()) {
                    if (processedRequests.get() >= totalRequests || outstandingRequests.get() == 0) {
                        logger.info("Processing complete as queue is empty with no outstanding requests. Exiting loop.");
                        break;
                    }
                    Thread.sleep(100);
                    continue;
                }

                if (!requestSemaphore.tryAcquire(1, 10, TimeUnit.SECONDS)) {
                    LOG.warn("WARN: Timed out waiting for semaphore permit. Will retry.");
                    continue;
                }

                if (requestHandler.isCompleted()) {
                    requestSemaphore.release();
                    break;
                }

                wrapper = processingQueue.poll();
                if (wrapper == null) {
                    requestSemaphore.release();
                    continue;
                }

                String instanceId = wrapper.userPrompt.getIssueData().getInstanceID();

                if (currentStreamState.processedIssueIds.contains(instanceId)) {
                    requestSemaphore.release();
                    continue;
                }

                if (wrapper.attemptCount == 0) {
                    outstandingRequests.incrementAndGet();
                }

                if (wrapper.attemptCount > 0) {
                    long delay = (long) (Constants.BASE_DELAY_MS * Math.pow(2, wrapper.attemptCount - 1));
                    delay = Math.min(delay, Constants.MAX_DELAY_MS) + ThreadLocalRandom.current().nextLong(100);
                    LOG.warn("WARN: Applying retry delay of {}ms for instance {} (attempt {}/{})",
                            delay, instanceId,
                            wrapper.attemptCount + 1, Constants.MAX_RETRIES);
                    Thread.sleep(delay);
                }

                LOG.info("Submitting request for instance " + instanceId);
                submitUserPrompt(wrapper);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                if (!resultFuture.isDone()) {
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Thread interrupted while processing queue", ie));
                }
                break;
            } catch (AviatorSimpleException e) {
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
                    outstandingRequests.decrementAndGet();
                    requestSemaphore.release();

                    logger.progress("Processed " + completed + " out of " + totalRequests + " issues (1 failed).");
                } else {
                    LOG.error("Caught AviatorSimpleException but the request wrapper was null.", e);
                }
            } catch (Exception e) {
                if (!resultFuture.isDone()) {
                    LOG.error("Unexpected error in processing loop, failing entire operation.", e);
                    resultFuture.completeExceptionally(new AviatorTechnicalException("Unexpected error in processing loop", e));
                }
                break;
            }
        }

        if (!resultFuture.isDone() && (processedRequests.get() >= totalRequests || (processingQueue.isEmpty() && outstandingRequests.get() == 0))) {
            logger.info("All requests have been accounted for. Completing operation and closing stream.");
            resultFuture.complete(responses);
            if (requestHandler != null && !requestHandler.isCompleted()) {
                requestHandler.complete();
            }
            if (streamLatch != null) {
                streamLatch.countDown();
            }
        }

        logger.info("Processing queue loop completed. Queue size: {}, Processed: {}/{}, Outstanding: {}",
                processingQueue.size(), processedRequests.get(), totalRequests, outstandingRequests.get());
    }

    private void handleServerBusy(String requestId, int totalRequests, AtomicInteger processedRequests, Map<String, AuditResponse> responses, CompletableFuture<Map<String, AuditResponse>> resultFuture, CountDownLatch streamLatch) {
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
            responses.put(wrapperToRetry.userPrompt.getIssueData().getInstanceID(), failedResponse);
            int completed = processedRequests.incrementAndGet();

            logger.progress("Request permanently failed due to server busy - Processed " + completed + " out of " + totalRequests + " issues");
            int stillOutstanding = outstandingRequests.decrementAndGet();
            LOG.warn("WARN: Request for instance {} permanently failed. Remaining outstanding requests: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), stillOutstanding);
            if (completed >= totalRequests) {
                logger.info("All requests accounted for after permanent failure, completing stream.");
                if (requestHandler != null && !requestHandler.isCompleted()) {
                    requestHandler.complete();
                }
                if (!resultFuture.isDone()) resultFuture.complete(responses);
                streamLatch.countDown();
            }
        } else {
            LOG.warn("WARN: Server is busy for instanceId {}. Re-queueing for retry (Attempt {}/{}). Queue size before re-queue: {}", wrapperToRetry.userPrompt.getIssueData().getInstanceID(), wrapperToRetry.attemptCount, Constants.MAX_RETRIES, processingQueue.size());

            processingQueue.addFirst(wrapperToRetry);

            logger.info("Request re-queued. Queue size after re-queue: {}", processingQueue.size());
        }
    }

    private void submitUserPrompt(RequestWrapper wrapper) {
        String requestId = UUID.randomUUID().toString();
        inflightRequests.put(requestId, wrapper);

        AuditRequest auditRequest = GrpcUtil.convertToAuditRequest(wrapper.userPrompt, currentStreamState.streamId, requestId);
        UserPromptRequest promptRequest = UserPromptRequest.newBuilder().setAudit(auditRequest).build();

        requestMetricsMap.put(requestId, new RequestMetrics());

        boolean sent = sendRequestWithRetry(promptRequest, Constants.MAX_RETRIES);
        if (!sent) {
            LOG.error("Failed to send request for instance {} after all retries. Re-queueing for later attempt.", wrapper.userPrompt.getIssueData().getInstanceID());
            inflightRequests.remove(requestId);
            requestMetricsMap.remove(requestId);
            outstandingRequests.decrementAndGet();
            requestSemaphore.release();
            wrapper.attemptCount++;
            processingQueue.addLast(wrapper);
        }
    }

    private boolean sendRequestWithRetry(UserPromptRequest request, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (requestHandler == null || !requestHandler.isReady()) {
                    LOG.warn("WARN: Request handler not ready for send (attempt {}), aborting", attempt + 1);
                    return false;
                }

                if (requestHandler.isCompleted()) {
                    LOG.warn("WARN: Request handler completed, cannot send (attempt {})", attempt + 1);
                    return false;
                }

                int messageSize = request.getSerializedSize();
                if (messageSize > Constants.MAX_MESSAGE_SIZE) {
                    LOG.error("Message size too large: {} bytes", messageSize);
                    throw new AviatorSimpleException("Message size exceeds maximum allowed limit");
                }

                LOG.info("Sending requestId: " + request.getAudit().getRequestId());
                requestHandler.sendRequest(request);
                return true;
            } catch (AviatorSimpleException e) {
                throw e;
            } catch (Exception e) {
                if (requestHandler != null && !requestHandler.isCompleted()) {
                    LOG.error("Error sending request (attempt {}): {}", attempt + 1, e.getMessage());
                }
                if (attempt == maxRetries - 1) {
                    return false;
                }
                try {
                    long baseBackoff = Constants.BASE_DELAY_MS * Math.min(10, currentBackoff.get() * (attempt + 1));
                    long jitter = ThreadLocalRandom.current().nextLong(100);
                    long backoffMs = Math.min(Constants.MAX_DELAY_MS, baseBackoff) + jitter;
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

                LOG.warn("WARN: Received multiple backpressure warnings. Reducing window to {} and setting backoff to {}x", newWindow, newBackoff);
            }
        } else {
            consecutiveBackpressureViolations.set(1);
        }
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

    private void sendPing() {
        try {
            if (requestHandler != null && requestHandler.isReady() && currentStreamState != null) {
                PingRequest pingRequest = PingRequest.newBuilder()
                        .setStreamId(currentStreamState.streamId)
                        .setTimestamp(System.currentTimeMillis())
                        .build();

                UserPromptRequest pingMsg = UserPromptRequest.newBuilder().setPing(pingRequest).build();
                requestHandler.getRequestQueue().addFirst(pingMsg);
                CompletableFuture.supplyAsync(() -> requestHandler.flush());
                LOG.info("ping  streamId: {}", currentStreamState.streamId);
            }
        } catch (Exception e) {
            if (requestHandler != null && !requestHandler.isCompleted()) {
                LOG.warn("WARN: Failed to send ping: {}", e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        stopPingPong();
        if (requestHandler != null && !requestHandler.isCompleted()) {
            requestHandler.complete();  // Flush remaining requests
        }
        if (streamLatch != null) {
            try {
                if (!streamLatch.await(10, TimeUnit.SECONDS)) {
                    LOG.warn("WARN: Timed out waiting for stream completion in close");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("WARN: Interrupted during close await");
            }
        }
    }
}