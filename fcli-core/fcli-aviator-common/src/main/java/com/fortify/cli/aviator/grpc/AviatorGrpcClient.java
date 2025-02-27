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

    private final IAviatorLogger logger;
    // Configuration constants
    private static final int MAX_MESSAGE_SIZE = 16 * 1024 * 1024;
    private static final int INITIAL_REQUEST_WINDOW = 20;
    private static final int SUBSEQUENT_REQUEST_WINDOW = 200;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 500;
    private static final long MAX_DELAY_MS = 2000;
    private final CountDownLatch latch = new CountDownLatch(1);
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
    private final Semaphore requestSemaphore = new Semaphore(INITIAL_REQUEST_WINDOW);
    private final AtomicInteger outstandingRequests = new AtomicInteger(0);
    private volatile StreamObserver<UserPromptRequest> requestObserver;
    private final AtomicBoolean streamCompleted = new AtomicBoolean(false);
    private volatile boolean isStreamActive = false;


    public AviatorGrpcClient(String host, int port, long timeoutMinutes, IAviatorLogger logger) {
        LOG.info("Initializing ImprovedGrpcClient - Host: " + host + ", Port: " + port);
        this.logger = logger;
        this.streamId = UUID.randomUUID().toString();

        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .maxInboundMessageSize(MAX_MESSAGE_SIZE)
                .keepAliveTime(30, TimeUnit.SECONDS)
                .keepAliveTimeout(10, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .enableRetry()
                .compressorRegistry(CompressorRegistry.getDefaultInstance())
                .decompressorRegistry(DecompressorRegistry.getDefaultInstance())
                .build();

        this.asyncStub = AuditorServiceGrpc.newStub(channel)
                .withCompression("gzip")
                .withMaxInboundMessageSize(MAX_MESSAGE_SIZE)
                .withMaxOutboundMessageSize(MAX_MESSAGE_SIZE)
                .withWaitForReady();

        this.blockingStub = ProjectServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(MAX_MESSAGE_SIZE).withWaitForReady();
        this.defaultTimeoutMinutes = timeoutMinutes;
        this.processingExecutor = Executors.newFixedThreadPool(2);
        this.isShutdown = new AtomicBoolean(false);
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(
            Queue<UserPrompt> requests, String projectName, String token) {
        isStreamActive = true;
        if (requests == null || requests.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Requests queue cannot be null or empty"));
        }

        logger.info("Starting processing - Total requests: " + requests.size());
        CompletableFuture<Map<String, AuditResponse>> resultFuture = new CompletableFuture<>();
        Map<String, AuditResponse> responses = new ConcurrentHashMap<>();
        AtomicInteger processedRequests = new AtomicInteger(0);
        int totalRequests = requests.size();

        StreamObserver<AuditorResponse> responseObserver = new StreamObserver<>() {
            private final AtomicBoolean isInitialized = new AtomicBoolean(false);

            @Override
            public void onNext(AuditorResponse response) {
                logger.progress("Received response - Status: " + response.getStatus() + ", RequestId: " + response.getRequestId());

                if (!isInitialized.get()) {
                    if ("SUCCESS".equals(response.getStatus())) {
                        isInitialized.set(true);
                        initLatch.countDown();
                        logger.info("Stream initialized successfully");
                    } else {
                        logger.progress("Stream initialization failed: " + response.getStatusMessage());
                        resultFuture.completeExceptionally(new RuntimeException("Stream initialization failed: " + response.getStatusMessage()));
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

                    logger.progress("Processed " + completed + " out of " + totalRequests + " requests");

                    if (completed >= totalRequests) {
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
                LOG.info("Stream error occurred: {}", t.getMessage());
                t.printStackTrace();
                if (!resultFuture.isDone()) {
                    resultFuture.completeExceptionally(t);
                }
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                logger.progress("Stream completed");
                if (!resultFuture.isDone()) {
                    resultFuture.complete(responses);
                }
                latch.countDown();
            }
        };

        requestObserver = asyncStub.processStream(responseObserver);

        try {
            LOG.info("Sending initialization request");
            String initRequestId = UUID.randomUUID().toString();
            UserPromptRequest initRequest = UserPromptRequest.newBuilder()
                    .setInit(StreamInitRequest.newBuilder()
                            .setStreamId(streamId)
                            .setRequestId(initRequestId)
                            .setToken(token)
                            .setProjectName(projectName)
                            .setTotalReportedIssues(totalRequests)
                            .setTotalIssuesToPredict(totalRequests)
                            .build())
                    .build();

            requestObserver.onNext(initRequest);

            processingExecutor.submit(() -> {
                try {
                    if (!initLatch.await(30, TimeUnit.SECONDS)) {
                        throw new TimeoutException("Stream initialization timed out");
                    }
                    processRequests(requests, requestObserver);
                } catch (Exception e) {
                    LOG.error("Error executing requests: " + e.getMessage());
                    e.printStackTrace();
                    if (!resultFuture.isDone()) {
                        resultFuture.completeExceptionally(e);
                    }
                    if (requestObserver != null && !streamCompleted.get()) {
                        requestObserver.onCompleted();
                    }
                }
            });

        } catch (Exception e) {
            LOG.error("Error during stream initialization: {}", e.getMessage());
            e.printStackTrace();
            resultFuture.completeExceptionally(e);
            if (requestObserver != null) {
                requestObserver.onCompleted();
            }
        }

        return resultFuture;
    }

    private void processRequests(Queue<UserPrompt> requests, StreamObserver<UserPromptRequest> observer) {
        logger.progress("Starting to process requests...");
        int totalProcessed = 0;
        AtomicInteger failedRequests = new AtomicInteger(0);

        while (!requests.isEmpty() && !isShutdown.get()) {
            try {
                requestSemaphore.acquire(); // Acquire a permit for backpressure control
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
                    requestSemaphore.release();
                    continue;
                }

                boolean sent = sendRequestWithRetry(promptRequest, MAX_RETRIES);
                if (!sent) {
                    failedRequests.incrementAndGet();
                    if (failedRequests.get() > 10) {
                        throw new RuntimeException("Too many failed requests");
                    }
                } else {
                    outstandingRequests.incrementAndGet();
                }

            } catch (Exception e) {
                LOG.error("Error processing request: " + e.getMessage());
                failedRequests.incrementAndGet();
            }
        }
    }

    private boolean sendRequestWithRetry(UserPromptRequest request, int maxRetries) {
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                if (requestObserver == null) {
                    LOG.error("Request observer is null, aborting send");
                    return false;
                }

                int messageSize = request.getSerializedSize();
                if (messageSize > MAX_MESSAGE_SIZE) {
                    LOG.error("Message size too large: {} bytes", messageSize);
                    return false;
                }

                requestObserver.onNext(request);
                return true;
            } catch (Exception e) {
                LOG.error("Error sending request (attempt " + (attempt + 1) + "): " + e.getMessage());
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

    @Override
    public void close() {
        LOG.debug("Closing client...");
        isShutdown.set(true);
        try {
            if (isStreamActive && !latch.await(10, TimeUnit.SECONDS)) {
                LOG.error("Timed out waiting for stream completion");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Interrupted while waiting for stream completion");
        }
        if (requestObserver != null && !streamCompleted.get()) {
            try {
                streamCompleted.set(true);
                requestObserver.onCompleted();
            } catch (Exception e) {
                LOG.error("Error closing request observer: " + e.getMessage());
            }
        }

        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
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
        AuditRequest.Builder builder = AuditRequest.newBuilder();
        builder.setIssueData(IssueData.newBuilder().setAccuracy(userPrompt.getIssueData().getAccuracy()).setAnalyzerName(userPrompt.getIssueData().getAnalyzerName()).setClassId(userPrompt.getIssueData().getClassID()).setConfidence(userPrompt.getIssueData().getConfidence()).setDefaultSeverity(userPrompt.getIssueData().getDefaultSeverity()).setImpact(userPrompt.getIssueData().getImpact()).setInstanceId(userPrompt.getIssueData().getInstanceID()).setInstanceSeverity(userPrompt.getIssueData().getInstanceSeverity()).setFiletype(userPrompt.getIssueData().getFiletype()).setKingdom(userPrompt.getIssueData().getKingdom()).setLikelihood(userPrompt.getIssueData().getLikelihood()).setPriority(userPrompt.getIssueData().getPriority()).setProbability(userPrompt.getIssueData().getProbability()).setSubType(userPrompt.getIssueData().getSubType()).setType(userPrompt.getIssueData().getType()).build());
        builder.setAnalysisInfo(AnalysisInfo.newBuilder().setShortDescription(userPrompt.getAnalysisInfo().getShortDescription()).setExplanation(userPrompt.getAnalysisInfo().getExplanation()).build());
        builder.addAllStackTrace(stackTraceElementLists);
        builder.addAllFirstStackTrace(userPrompt.getFirstStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList()));
        builder.addAllLongestStackTrace(userPrompt.getLongestStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList()));
        builder.addAllFiles(userPrompt.getFiles().stream().map(file -> File.newBuilder().setName(file.getName()).setContent(file.getContent()).setSegment(file.isSegment()).setStartLine(file.getStartLine()).setEndLine(file.getEndLine()).build()).collect(Collectors.toList()));
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

        return com.fortify.aviator.grpc.StackTraceElement.newBuilder().setFilename(element.getFilename()).setLine(element.getLine()).setCode(element.getCode()).setNodeType(element.getNodeType()).setFragment(Fragment.newBuilder().setContent(element.getFragment().getContent()).setStartLine(element.getFragment().getStartLine()).setEndLine(element.getFragment().getEndLine()).build()).setAdditionalInfo(element.getAdditionalInfo()).setTaintflags(element.getTaintflags() == null ? "" : element.getTaintflags()).addAllInnerStackTrace(element.getInnerStackTrace().stream().map(this::convertToStackTraceElement).collect(Collectors.toList())).build();
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