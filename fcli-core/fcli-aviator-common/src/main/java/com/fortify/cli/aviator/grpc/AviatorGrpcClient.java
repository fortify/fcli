package com.fortify.cli.aviator.grpc;

import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.aviator.entitlement.EntitlementServiceGrpc;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantRequest;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantResponse;
import com.fortify.aviator.grpc.*;
import com.fortify.aviator.project.CreateProjectRequest;
import com.fortify.aviator.project.Project;
import com.fortify.aviator.project.ProjectById;
import com.fortify.aviator.project.ProjectByTenantName;
import com.fortify.aviator.project.ProjectList;
import com.fortify.aviator.project.ProjectResponseMessage;
import com.fortify.aviator.project.ProjectServiceGrpc;
import com.fortify.aviator.project.UpdateProjectRequest;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.core.model.StackTraceElement;
import com.fortify.cli.aviator.core.model.UserPrompt;
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
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class AviatorGrpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClient.class);

    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024; // 16MB
    private static final int BATCH_SIZE = 3;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final long MAX_DELAY_MS = 2000;

    private final IAviatorLogger logger;
    private final ManagedChannel channel;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final ProjectServiceGrpc.ProjectServiceBlockingStub blockingStub;
    private final TokenServiceGrpc.TokenServiceBlockingStub tokenServiceBlockingStub;
    private final EntitlementServiceGrpc.EntitlementServiceBlockingStub entitlementServiceBlockingStub;
    private final String streamId;
    private final long defaultTimeoutMinutes;
    private final ExecutorService processingExecutor;
    private final AtomicBoolean isShutdown;
    private final CountDownLatch initLatch = new CountDownLatch(1);
    private final Semaphore requestSemaphore = new Semaphore(1);
    private volatile StreamObserver<UserPromptRequest> requestObserver;

    public AviatorGrpcClient(String host, int port, long timeoutMinutes, IAviatorLogger logger) {
        LOG.info("Initializing ImprovedGrpcClient - Host: {}, Port: {}", host, port);
        this.logger = logger;
        this.streamId = UUID.randomUUID().toString();

        this.channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext().maxInboundMessageSize(MAX_MESSAGE_SIZE).keepAliveTime(30, TimeUnit.SECONDS).keepAliveTimeout(10, TimeUnit.SECONDS).keepAliveWithoutCalls(true).enableRetry().compressorRegistry(CompressorRegistry.getDefaultInstance()).decompressorRegistry(DecompressorRegistry.getDefaultInstance()).build();

        this.asyncStub = AuditorServiceGrpc.newStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.blockingStub = ProjectServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.defaultTimeoutMinutes = timeoutMinutes;
        this.processingExecutor = Executors.newFixedThreadPool(2);
        this.isShutdown = new AtomicBoolean(false);
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(Queue<UserPrompt> requests, String tenantId, String tokenName, String projectId, String entitlementId) {

        if (requests == null || requests.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Requests queue cannot be null or empty"));
        }

        logger.info("Starting batch processing - Total requests: {}", requests.size());
        CompletableFuture<Map<String, AuditResponse>> resultFuture = new CompletableFuture<>();
        Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        AtomicInteger processedRequests = new AtomicInteger(0);
        int totalRequests = requests.size();

        StreamObserver<AuditorResponse> responseObserver = new StreamObserver<>() {
            private final AtomicBoolean isInitialized = new AtomicBoolean(false);

            @Override
            public void onNext(AuditorResponse response) {
                LOG.debug("Received response - Status: {}, RequestId: {}", response.getStatus(), response.getRequestId());

                if (!isInitialized.get()) {
                    if ("SUCCESS".equals(response.getStatus())) {
                        isInitialized.set(true);
                        initLatch.countDown();
                        logger.progress("Stream initialized successfully");
                    } else {
                        logger.progress("Stream initialization failed: %s", response.getStatusMessage());
                        resultFuture.completeExceptionally(new RuntimeException("Stream initialization failed: " + response.getStatusMessage()));
                    }
                } else {
                    AuditResponse auditResponse = convertToAuditResponse(response);
                    responses.put(response.getRequestId(), auditResponse);
                    int completed = processedRequests.incrementAndGet();
                    logger.progress("Processed %d out of %d requests", completed, totalRequests);

                    if (completed >= totalRequests) {
                        LOG.info("All requests processed, completing stream");
                        requestObserver.onCompleted();
                        if (!resultFuture.isDone()) {
                            resultFuture.complete(responses);
                        }
                    }
                }
            }

            @Override
            public void onError(Throwable t) {
                LOG.info("Stream error occurred: {}", t.getMessage());
                t.printStackTrace();
                resultFuture.completeExceptionally(t);
            }

            @Override
            public void onCompleted() {
                LOG.info("Stream completed");
                if (!resultFuture.isDone()) {
                    resultFuture.complete(responses);
                }
            }
        };

        requestObserver = asyncStub.processStream(responseObserver);

        try {
            LOG.info("Sending initialization request");
            String initRequestId = UUID.randomUUID().toString();
            UserPromptRequest initRequest = UserPromptRequest.newBuilder().setInit(StreamInitRequest.newBuilder().setStreamId(streamId).setRequestId(initRequestId).setTenantId(tenantId).setTokenName(tokenName).setProjectId(projectId).setEntitlementId(entitlementId).build()).build();

            requestObserver.onNext(initRequest);

            processingExecutor.submit(() -> {
                try {
                    if (!initLatch.await(30, TimeUnit.SECONDS)) {
                        throw new TimeoutException("Stream initialization timed out");
                    }

                    processRequests(requests, requestObserver);
                } catch (Exception e) {
                    LOG.info("Error executing requests: {}", e.getMessage());
                    e.printStackTrace();
                    resultFuture.completeExceptionally(e);
                    requestObserver.onCompleted();
                }
            });

        } catch (Exception e) {
            LOG.info("Error during stream initialization: {}", e.getMessage());
            e.printStackTrace();
            resultFuture.completeExceptionally(e);
            requestObserver.onCompleted();
        }

        scheduleTimeout(resultFuture);
        return resultFuture;
    }

    private int calculateDynamicBatchSize(int messageSize) {
        if (messageSize > 15000) {
            return 3;  // For large messages
        } else if (messageSize > 10000) {
            return 4;  // For medium messages
        } else {
            return 5;  // For smaller messages
        }
    }

    private void processRequests(Queue<UserPrompt> requests, StreamObserver<UserPromptRequest> observer) {
        LOG.info("Starting to process requests...");
        int totalProcessed = 0;
        AtomicInteger failedRequests = new AtomicInteger(0);
        int currentBatchSize = BATCH_SIZE;

        while (!requests.isEmpty() && !isShutdown.get()) {
            UserPrompt request = requests.peek();
            if (request != null) {
                AuditRequest testAuditRequest = convertToAuditRequest(request, streamId, UUID.randomUUID().toString());
                UserPromptRequest testRequest = UserPromptRequest.newBuilder().setAudit(testAuditRequest).build();
                int messageSize = testRequest.getSerializedSize();
                currentBatchSize = calculateDynamicBatchSize(messageSize);
            }

            int batchSize = Math.min(currentBatchSize, requests.size());
            LOG.debug("Processing batch with size {} (Remaining: {})", batchSize, requests.size());

            processRequestBatch(requests, observer, batchSize, totalProcessed, failedRequests);

            try {
                long delayMs = currentBatchSize < BATCH_SIZE ? MAX_DELAY_MS : BASE_DELAY_MS;
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processRequestBatch(Queue<UserPrompt> requests, StreamObserver<UserPromptRequest> observer, int batchSize, int totalProcessed, AtomicInteger failedRequests) {
        for (int i = 0; i < batchSize && !requests.isEmpty(); i++) {
            UserPrompt request = requests.poll();
            if (request == null) break;

            try {
                String requestId = UUID.randomUUID().toString();
                AuditRequest auditRequest = convertToAuditRequest(request, streamId, requestId);
                UserPromptRequest promptRequest = UserPromptRequest.newBuilder().setAudit(auditRequest).build();

                int messageSize = promptRequest.getSerializedSize();
                LOG.debug("Request size: {} bytes", messageSize);

                if (messageSize > MAX_MESSAGE_SIZE) {
                    LOG.info("Request too large, skipping");
                    continue;
                }

                long delayMs = messageSize > 10000 ? 750 : 500;
                Thread.sleep(delayMs);

                boolean sent = sendRequestWithRetry(promptRequest, MAX_RETRIES);
                if (!sent) {
                    failedRequests.incrementAndGet();
                    if (failedRequests.get() > 10) {
                        throw new RuntimeException("Too many failed requests");
                    }
                }

            } catch (Exception e) {
                LOG.error("Error processing request: {}", e.getMessage());
                failedRequests.incrementAndGet();
            }
        }
    }

    private boolean sendRequestWithRetry(UserPromptRequest request, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                requestSemaphore.acquire();
                try {
                    if (requestObserver == null) {
                        LOG.debug("Request observer is null, aborting send");
                        return false;
                    }

                    int messageSize = request.getSerializedSize();
                    if (messageSize > MAX_MESSAGE_SIZE) {
                        LOG.debug("Message size too large: {} bytes", messageSize);
                        return false;
                    }


                    requestObserver.onNext(request);
                    return true;
                } finally {
                    requestSemaphore.release();
                }
            } catch (Exception e) {
                LOG.error("Error sending request (attempt {}): {}", attempt + 1, e.getMessage());
                if (attempt == maxRetries - 1) {
                    return false;
                }
                try {
                    long backoffMs = Math.min(1000 * (long) Math.pow(2, attempt), 10000);
                    backoffMs += ThreadLocalRandom.current().nextLong(100);
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    private void scheduleTimeout(CompletableFuture<Map<String, AuditResponse>> future) {
        CompletableFuture.runAsync(() -> {
            try {
                if (!future.isDone() && !future.get(defaultTimeoutMinutes, TimeUnit.MINUTES).isEmpty()) {
                    String timeoutMsg = "Request processing timed out after " + defaultTimeoutMinutes + " minutes";
                    LOG.info(timeoutMsg);
                    future.completeExceptionally(new TimeoutException(timeoutMsg));
                    if (requestObserver != null) {
                        requestObserver.onCompleted();
                    }
                }
            } catch (Exception e) {
                LOG.error("Timeout or error occurred: {}", e.getMessage());
                future.completeExceptionally(e);
                if (requestObserver != null) {
                    requestObserver.onCompleted();
                }
            }
        }, processingExecutor);
    }

    @Override
    public void close() {
        LOG.info("Closing client...");
        isShutdown.set(true);

        if (requestObserver != null) {
            try {
                requestSemaphore.acquire();
                try {
                    requestObserver.onCompleted();
                } finally {
                    requestSemaphore.release();
                }
            } catch (Exception e) {
                LOG.error("Error closing request observer: {}", e.getMessage());
            }
        }

        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                channel.shutdownNow();
                processingExecutor.shutdown();
                try {
                    if (!processingExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        processingExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    processingExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
        LOG.info("Client closed");
    }

    private AuditRequest convertToAuditRequest(UserPrompt userPrompt, String streamId, String requestId) {

        List<StackTraceElementList> stackTraceElementLists = new ArrayList<>();
        for (List<StackTraceElement> innerList : userPrompt.getStackTrace()) {
            StackTraceElementList stackTraceElementList = StackTraceElementList.newBuilder().addAllElements(innerList.stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).build();
            stackTraceElementLists.add(stackTraceElementList);
        }
        return AuditRequest.newBuilder().setIssueData(IssueData.newBuilder().setAccuracy(userPrompt.getIssueData().getAccuracy()).setAnalyzerName(userPrompt.getIssueData().getAnalyzerName()).setClassId(userPrompt.getIssueData().getClassID()).setConfidence(userPrompt.getIssueData().getConfidence()).setDefaultSeverity(userPrompt.getIssueData().getDefaultSeverity()).setImpact(userPrompt.getIssueData().getImpact()).setInstanceId(userPrompt.getIssueData().getInstanceID()).setInstanceSeverity(userPrompt.getIssueData().getInstanceSeverity()).setFiletype(userPrompt.getIssueData().getFiletype()).setKingdom(userPrompt.getIssueData().getKingdom()).setLikelihood(userPrompt.getIssueData().getLikelihood()).setPriority(userPrompt.getIssueData().getPriority()).setProbability(userPrompt.getIssueData().getProbability()).setSubType(userPrompt.getIssueData().getSubType()).setType(userPrompt.getIssueData().getType()).build()).setAnalysisInfo(AnalysisInfo.newBuilder().setShortDescription(userPrompt.getAnalysisInfo().getShortDescription()).setExplanation(userPrompt.getAnalysisInfo().getExplanation()).build()).addAllStackTrace(stackTraceElementLists).addAllFirstStackTrace(userPrompt.getFirstStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).addAllLongestStackTrace(userPrompt.getLongestStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).addAllFiles(userPrompt.getFiles().stream().map(file -> File.newBuilder().setName(file.getName()).setContent(file.getContent()).setSegment(file.isSegment()).setStartLine(file.getStartLine()).setEndLine(file.getEndLine()).build()).collect(Collectors.toList())).setLastStackTraceElement(convertToStackTraceElement(userPrompt.getLastStackTraceElement())).addAllProgrammingLanguages(userPrompt.getProgrammingLanguages()).setFileExtension(userPrompt.getFileExtension()).setLanguage(userPrompt.getLanguage()).setCategory(userPrompt.getCategory() == null ? "" : userPrompt.getCategory()).setSource(convertToStackTraceElement(userPrompt.getSource())).setSink(convertToStackTraceElement(userPrompt.getSink())).setCategoryLevel(userPrompt.getCategoryLevel() == null ? "" : userPrompt.getCategoryLevel()).setRequestId(requestId).setStreamId(streamId).build();
    }


    private com.fortify.aviator.grpc.StackTraceElement convertToStackTraceElement(StackTraceElement element) {
        if (element == null) return null;

        return com.fortify.aviator.grpc.StackTraceElement.newBuilder().setFilename(element.getFilename()).setLine(element.getLine()).setCode(element.getCode()).setNodeType(element.getNodeType()).setFragment(Fragment.newBuilder().setContent(element.getFragment().getContent()).setStartLine(element.getFragment().getStartLine()).setEndLine(element.getFragment().getEndLine()).build()).setAdditionalInfo(element.getAdditionalInfo()).setTaintflags(element.getTaintflags() == null ? "" : element.getTaintflags()).addAllInnerStackTrace(element.getInnerStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).build();
    }

    private interface WindowProcessor {
        void processNextWindow(Queue<UserPrompt> requests);
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
//        auditResponse.setRequestId(response.getRequestId());
//        auditResponse.setStreamId(response.getStreamId());
        return auditResponse;
    }

    public Project createProject(String name, String tenantName, String signature, String message) {
        CreateProjectRequest request = CreateProjectRequest.newBuilder().setName(name).setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        try {
            return blockingStub.createProject(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error creating project: " + e.getStatus(), e);
        }
    }

    public Project updateProject(String projectId, String newName, String signature, String message, String tenantName) {
        UpdateProjectRequest request = UpdateProjectRequest.newBuilder()
                .setId(Long.parseLong(projectId))
                .setName(newName)
                .setTenantName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        try {
            return blockingStub.updateProject(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error updating project: " + e.getStatus(), e);
        }
    }

    public ProjectResponseMessage deleteProject(String projectId, String signature, String message, String tenantName) {
        ProjectById request = ProjectById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        try {
            return blockingStub.deleteProject(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error deleting project: " + e.getStatus(), e);
        }
    }
    public Project getProject(String projectId, String signature, String message, String tenantName) {
        ProjectById request = ProjectById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build(); // Corrected to use ProjectById and parse the ID as a long
        try {
            return blockingStub.getProject(request);
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error getting project: " + e.getStatus(), e);
        }
    }
    public List<Project> listProjects(String tenantName, String signature, String message) {
        ProjectByTenantName request = ProjectByTenantName.newBuilder().setName(tenantName).setSignature(signature).setMessage(message).build();
        try {
            ProjectList projectList = blockingStub.listProjects(request);
            return projectList.getProjectsList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error listing projects: " + e.getStatus(), e);
        }
    }
    public TokenGenerationResponse generateToken(String email, String tokenName, String signature, String message, String tenantName, String endDate) {
        TokenGenerationRequest.Builder requestBuilder = TokenGenerationRequest.newBuilder()
                .setEmail(email != null ? email : "")
                .setCustomTokenName(tokenName != null ? tokenName : "")
                .setRequestSignature(signature)
                .setMessage(message)
                .setTenantName(tenantName);
//        // Only set end date if it's not null
//        if (endDate != null) {
//            requestBuilder.setEndDate(endDate);
//        }
        try {
            TokenGenerationResponse response = tokenServiceBlockingStub.generateToken(requestBuilder.build());
            return response;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error generating token " + e.getStatus(), e);
        }
    }

    public ListTokensResponse listTokens(String email, String tenantName, String signature, String message, int page_size, String pageToken) {
        ListTokensRequest request = ListTokensRequest.newBuilder().setEmail(email).setRequestSignature(signature).setMessage(message).setTenantName(tenantName).setPageSize(page_size).setPageToken(pageToken).build();
        try {
            ListTokensResponse response = tokenServiceBlockingStub.listTokens(request);
            return response;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error listing tokens " + e.getStatus(), e);
        }
    }

    public RevokeTokenResponse revokeToken(String token, String email, String tenantName, String signature, String message) {
        RevokeTokenRequest request = RevokeTokenRequest.newBuilder().setToken(token).setEmail(email).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        try {
            RevokeTokenResponse response = tokenServiceBlockingStub.revokeToken(request);
            return response;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error revoking tokens " + e.getStatus(), e);
        }
    }

    public DeleteTokenResponse deleteToken(String token, String email, String tenantName, String signature, String message) {
        DeleteTokenRequest request = DeleteTokenRequest.newBuilder().setToken(token).setEmail(email).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        try {
            DeleteTokenResponse response = tokenServiceBlockingStub.deleteToken(request);
            return response;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error deleting tokens " + e.getStatus(), e);
        }
    }

    public TokenValidationResponse validateToken(String token, String tenantName, String signature, String message) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        try {
            TokenValidationResponse response = tokenServiceBlockingStub.validateToken(request);
            return response;
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error validating token " + e.getStatus(), e);
        }
    }

    public List<Entitlement> listEntitlements(String tenantName, String signature, String message) {
        ListEntitlementsByTenantRequest request = ListEntitlementsByTenantRequest.newBuilder()
                .setTenantName(tenantName)
                .setSignature(signature)
                .setMessage(message)
                .build();
        try {
            ListEntitlementsByTenantResponse response = entitlementServiceBlockingStub.listEntitlementsByTenant(request);
            return response.getEntitlementsList();
        } catch (StatusRuntimeException e) {
            throw new RuntimeException("Error listing entitlements: " + e.getStatus(), e);
        }
    }
}
