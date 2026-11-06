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

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.application.Application;
import com.fortify.aviator.application.ApplicationById;
import com.fortify.aviator.application.ApplicationByName;
import com.fortify.aviator.application.ApplicationByTenantName;
import com.fortify.aviator.application.ApplicationList;
import com.fortify.aviator.application.ApplicationMultiplierOperationResponse;
import com.fortify.aviator.application.ApplicationResponseMessage;
import com.fortify.aviator.application.ApplicationServiceGrpc;
import com.fortify.aviator.application.CreateApplicationRequest;
import com.fortify.aviator.application.GetApplicationByTokenRequest;
import com.fortify.aviator.application.GetDefaultQuotaRequest;
import com.fortify.aviator.application.GetDefaultQuotaResponse;
import com.fortify.aviator.application.ServerCapabilities;
import com.fortify.aviator.application.UpdateApplicationRequest;
import com.fortify.aviator.application.ValidateAdminSessionRequest;
import com.fortify.aviator.dastaudit.DastAuditServiceGrpc;
import com.fortify.aviator.dastentitlement.DastEntitlement;
import com.fortify.aviator.dastentitlement.DastEntitlementServiceGrpc;
import com.fortify.aviator.dastentitlement.ListDastEntitlementsByTenantRequest;
import com.fortify.aviator.dastentitlement.ListDastEntitlementsByTenantResponse;
import com.fortify.aviator.entitlement.Entitlement;
import com.fortify.aviator.entitlement.EntitlementServiceGrpc;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantRequest;
import com.fortify.aviator.entitlement.ListEntitlementsByTenantResponse;
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.aviator.grpc.CorrelationServiceGrpc;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.model.FVDLMetadata;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;
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

public class AviatorGrpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClient.class);

    private final IAviatorLogger logger;
    private final ManagedChannel channel;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final ApplicationServiceGrpc.ApplicationServiceBlockingStub blockingStub;
    private final TokenServiceGrpc.TokenServiceBlockingStub tokenServiceBlockingStub;
    private final EntitlementServiceGrpc.EntitlementServiceBlockingStub entitlementServiceBlockingStub;
    private final DastEntitlementServiceGrpc.DastEntitlementServiceBlockingStub dastEntitlementServiceBlockingStub;
    private final CorrelationServiceGrpc.CorrelationServiceStub correlationAsyncStub;
    private final DastAuditServiceGrpc.DastAuditServiceStub dastAuditAsyncStub;
    private final long defaultTimeoutSeconds;
    private final java.util.concurrent.ExecutorService processingExecutor;
    private final long pingIntervalSeconds;
    private final java.util.concurrent.ScheduledExecutorService pingScheduler;
    final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        LOG.debug("Initializing AviatorGrpcClient with ManagedChannel");
        this.logger = logger;
        this.channel = channel;
        this.asyncStub = AuditorServiceGrpc.newStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.blockingStub = ApplicationServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.dastEntitlementServiceBlockingStub = DastEntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.correlationAsyncStub = CorrelationServiceGrpc.newStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.dastAuditAsyncStub = DastAuditServiceGrpc.newStub(channel)
            .withCompression("gzip")
            .withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE)
            .withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE)
            .withWaitForReady();
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
        this.processingExecutor = Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "aviator-client-processing-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
        this.pingIntervalSeconds = pingIntervalSeconds;
        this.pingScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "aviator-client-ping-" + r.hashCode());
            t.setDaemon(true);
            return t;
        });
    }

    public AviatorGrpcClient(String host, int port, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        this(ManagedChannelBuilder.forAddress(host, port).useTransportSecurity().maxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).keepAliveTime(30, TimeUnit.SECONDS).keepAliveTimeout(10, TimeUnit.SECONDS).keepAliveWithoutCalls(true).enableRetry().compressorRegistry(CompressorRegistry.getDefaultInstance()).decompressorRegistry(DecompressorRegistry.getDefaultInstance()).build(), defaultTimeoutSeconds, logger, pingIntervalSeconds);
        LOG.debug("Initialized AviatorGrpcClient - Host: {}, Port: {}", host, port);
    }

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger) {
        this(channel, defaultTimeoutSeconds, logger, 30);
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(Queue<UserPrompt> requests, String projectName,
            String FPRBuildId, String SSCApplicationName, String SSCApplicationVersion, String token, FprHandle fprHandle,
            List<String> customPriorityOrder, ISourceDecoder sourceDecoder, FVDLMetadata fvdlMetadata) {
        AviatorStreamProcessor processor = new AviatorStreamProcessor(this, logger, asyncStub, processingExecutor, pingScheduler,
                pingIntervalSeconds, defaultTimeoutSeconds, fprHandle, sourceDecoder, fvdlMetadata);
        CompletableFuture<Map<String, AuditResponse>> future = processor.processBatchRequests(requests, projectName, FPRBuildId, SSCApplicationName, SSCApplicationVersion, token, customPriorityOrder);
        future.whenComplete((res, th) -> processor.close());
        return future.exceptionally(ex -> {
            Throwable cause = (ex instanceof CompletionException || ex instanceof ExecutionException) && ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof AviatorSimpleException) throw (AviatorSimpleException) cause;
            if (cause instanceof AviatorTechnicalException) throw (AviatorTechnicalException) cause;
            throw new AviatorTechnicalException("Processing FPR failed", cause);
        });
    }

    @Override
    public void close() {
        LOG.debug("Closing client...");
        isShutdown.set(true);
        if (channel != null && !channel.isShutdown()) {
            try {
                channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("WARN: Interrupted during channel shutdown");
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
                    LOG.warn("WARN: Interrupted during executor shutdown");
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

        LOG.debug("Client closed");
    }

    // ------------------------------------------------------------------------------------------------
    // Replay protection: capability negotiation and state-changing calls
    // ------------------------------------------------------------------------------------------------

    private volatile Optional<ServerCapabilities> serverCapabilities;

    /**
     * Capabilities advertised by the server, or empty when the server predates replay protection
     * ({@code UNIMPLEMENTED}). Cached for the lifetime of the client.
     */
    public Optional<ServerCapabilities> getServerCapabilities() {
        Optional<ServerCapabilities> cached = serverCapabilities;
        if (cached == null) {
            try {
                cached = Optional.of(blockingStub.withDeadlineAfter(Constants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                        .getServerCapabilities(com.google.protobuf.Empty.getDefaultInstance()));
            } catch (StatusRuntimeException e) {
                if (e.getStatus().getCode() != Status.Code.UNIMPLEMENTED) {
                    throw GrpcUtil.toAviatorException(e, Constants.OP_GET_SERVER_CAPABILITIES, null);
                }
                cached = Optional.empty();
            }
            serverCapabilities = cached;
        }
        return cached;
    }

    /**
     * Chooses the operation id for one logical operation. A user supplied id (a retry of an earlier run) is
     * validated; otherwise a new UUIDv7 is generated. Returns {@code null} when the server does not support
     * operation ids, in which case the legacy signed message is used and the call is never retried.
     */
    String resolveOperationId(String requestedOperationId) {
        Optional<ServerCapabilities> capabilities = getServerCapabilities();
        if (capabilities.isEmpty()) {
            if (requestedOperationId != null) {
                throw new AviatorSimpleException("This Aviator server does not support operation IDs, so --operation-id cannot be honored. "
                        + "Upgrade the Aviator server, or omit --operation-id (the command then cannot be retried safely).");
            }
            LOG.warn("The Aviator server does not support operation IDs; this request cannot be retried safely after an unknown outcome.");
            return null;
        }
        if (requestedOperationId == null) {
            return AviatorOperationId.generate();
        }
        if (!AviatorOperationId.isValid(requestedOperationId)) {
            throw new AviatorSimpleException("Invalid --operation-id '" + requestedOperationId
                    + "'. Use the exact operation ID printed by the failed run (a lowercase UUIDv7).");
        }
        long maxAgeSeconds = capabilities.get().getOperationIdMaxAgeSeconds();
        Duration maxAge = maxAgeSeconds > 0 ? Duration.ofSeconds(maxAgeSeconds) : AviatorOperationId.DEFAULT_MAX_AGE;
        if (AviatorOperationId.isExpired(requestedOperationId, maxAge, Clock.systemUTC())) {
            throw new AviatorSimpleException("Operation ID " + requestedOperationId + " is older than " + maxAge.toHours()
                    + " hours and can no longer be deduplicated by the server. Check the current state with the corresponding "
                    + "list or get command; if the change was not applied, run the command again without --operation-id.");
        }
        return requestedOperationId;
    }

    private <S extends io.grpc.stub.AbstractBlockingStub<S>, T, R> R executeMutation(S stub, String operation, String fullMethodName,
            String requestedOperationId, AviatorRequestSigner signer,
            java.util.function.BiFunction<AviatorRequestSigner.SignedRequest, String, T> requestFactory,
            GrpcUtil.GrpcCall<S, T, R> call) {
        String operationId = resolveOperationId(requestedOperationId);
        if (operationId != null) {
            LOG.info("{} uses operation ID {}", operation, operationId);
        }
        return GrpcUtil.executeMutationCall(stub, call,
                () -> requestFactory.apply(signer.sign(fullMethodName, operationId), operationId), operation, operationId);
    }

    public Application createApplication(String name, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(blockingStub, Constants.OP_CREATE_APP,
                ApplicationServiceGrpc.getCreateApplicationMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> withOperationId(CreateApplicationRequest.newBuilder().setName(name).setTenantName(tenantName)
                        .setSignature(signed.signature()).setMessage(signed.message()), operationId),
                ApplicationServiceGrpc.ApplicationServiceBlockingStub::createApplication);
    }

    public Application updateApplication(String projectId, String newName, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(blockingStub, Constants.OP_UPDATE_APP,
                ApplicationServiceGrpc.getUpdateApplicationMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> withOperationId(UpdateApplicationRequest.newBuilder().setId(Long.parseLong(projectId)).setName(newName)
                        .setTenantName(tenantName).setSignature(signed.signature()).setMessage(signed.message()), operationId),
                ApplicationServiceGrpc.ApplicationServiceBlockingStub::updateApplication);
    }

    public Application addEntitlement(String projectId, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(blockingStub, Constants.OP_ADD_APP_ENTITLEMENT,
                ApplicationServiceGrpc.getAddEntitlementMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> applicationById(projectId, tenantName, signed, operationId),
                ApplicationServiceGrpc.ApplicationServiceBlockingStub::addEntitlement);
    }

    public ApplicationMultiplierOperationResponse reduceEntitlement(String projectId, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(blockingStub, Constants.OP_REDUCE_APP_ENTITLEMENT,
                ApplicationServiceGrpc.getReduceEntitlementMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> applicationById(projectId, tenantName, signed, operationId),
                ApplicationServiceGrpc.ApplicationServiceBlockingStub::reduceEntitlement);
    }

    public ApplicationResponseMessage deleteApplication(String projectId, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(blockingStub, Constants.OP_DELETE_APP,
                ApplicationServiceGrpc.getDeleteApplicationMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> applicationById(projectId, tenantName, signed, operationId),
                ApplicationServiceGrpc.ApplicationServiceBlockingStub::deleteApplication);
    }

    private static ApplicationById applicationById(String projectId, String tenantName, AviatorRequestSigner.SignedRequest signed, String operationId) {
        ApplicationById.Builder builder = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signed.signature())
                .setMessage(signed.message()).setTenantName(tenantName);
        if (operationId != null) {
            builder.setOperationId(operationId);
        }
        return builder.build();
    }

    private static CreateApplicationRequest withOperationId(CreateApplicationRequest.Builder builder, String operationId) {
        return operationId == null ? builder.build() : builder.setOperationId(operationId).build();
    }

    private static UpdateApplicationRequest withOperationId(UpdateApplicationRequest.Builder builder, String operationId) {
        return operationId == null ? builder.build() : builder.setOperationId(operationId).build();
    }

    public Application getApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplication, request, Constants.OP_GET_APP);
    }

    public Application getApplicationByName(String applicationName, String signature, String message, String tenantName) {
        ApplicationByName request = ApplicationByName.newBuilder().setName(applicationName).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplicationByName, request, Constants.OP_GET_APP_BY_NAME,
                "Specify the numeric application id instead of the application name.");
    }

    public Application getApplicationByToken(String token, String appName) {
        GetApplicationByTokenRequest request = GetApplicationByTokenRequest.newBuilder()
            .setToken(token)
            .setAppName(appName)
            .build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplicationByToken, request, Constants.OP_GET_APP_BY_TOKEN);
    }

    public long getDefaultQuota(String token) {
        GetDefaultQuotaRequest request = GetDefaultQuotaRequest.newBuilder()
            .setToken(token)
            .build();
        GetDefaultQuotaResponse response = GrpcUtil.executeGrpcCall(blockingStub,
            ApplicationServiceGrpc.ApplicationServiceBlockingStub::getDefaultQuota,
            request, Constants.OP_GET_DEFAULT_QUOTA);
        return response.getDefaultQuota();
    }

    public void probeGetDefaultQuota(long timeoutSeconds) {
        blockingStub.withDeadlineAfter(timeoutSeconds, TimeUnit.SECONDS)
            .getDefaultQuota(GetDefaultQuotaRequest.getDefaultInstance());
    }

    public void validateAdminSession(String tenantName, String signature, String message) {
        ValidateAdminSessionRequest request = ValidateAdminSessionRequest.newBuilder()
            .setTenantName(tenantName)
            .setSignature(signature)
            .setMessage(message)
            .build();
        GrpcUtil.executeGrpcCall(
            blockingStub,
            ApplicationServiceGrpc.ApplicationServiceBlockingStub::validateAdminSession,
            request,
            Constants.OP_VALIDATE_ADMIN_SESSION);
    }

    public List<Application> listApplication(String tenantName, String signature, String message) {
        ApplicationByTenantName request = ApplicationByTenantName.newBuilder().setName(tenantName).setSignature(signature).setMessage(message).build();
        ApplicationList applicationList = GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::listApplications, request, Constants.OP_LIST_APPS);
        return applicationList.getApplicationsList();
    }

    public TokenGenerationResponse generateToken(String email, String tokenName, String endDate, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(tokenServiceBlockingStub, Constants.OP_GENERATE_TOKEN,
                TokenServiceGrpc.getGenerateTokenMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> {
                    TokenGenerationRequest.Builder builder = TokenGenerationRequest.newBuilder().setEmail(email != null ? email : "")
                            .setCustomTokenName(tokenName != null ? tokenName : "").setRequestSignature(signed.signature())
                            .setEndDate(StringUtil.isEmpty(endDate) ? "" : endDate).setMessage(signed.message()).setTenantName(tenantName);
                    return operationId == null ? builder.build() : builder.setOperationId(operationId).build();
                },
                TokenServiceGrpc.TokenServiceBlockingStub::generateToken);
    }

    public ListTokensResponse listTokens(String email, String tenantName, String signature, String message) {
        ListTokensRequest request = ListTokensRequest.newBuilder().setRequestSignature(signature).setMessage(message).setTenantName(tenantName).setIgnorePagination(true).build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::listTokens, request, Constants.OP_LIST_TOKENS);
    }

    public ListTokensResponse listTokensByDeveloper(String tenantName, String developerEmail, String signature, String message) {
        ListTokensByDeveloperRequest.Builder requestBuilder = ListTokensByDeveloperRequest.newBuilder().setTenantName(tenantName).setRequestSignature(signature).setMessage(message).setIgnorePagination(true);
        if (developerEmail != null) {
            requestBuilder.setDeveloperEmail(developerEmail);
        }
        ListTokensByDeveloperRequest request = requestBuilder.build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::listTokensByDeveloper, request, Constants.OP_LIST_TOKENS_BY_DEVELOPER);
    }

    public RevokeTokenResponse revokeToken(String token, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(tokenServiceBlockingStub, Constants.OP_REVOKE_TOKEN,
                TokenServiceGrpc.getRevokeTokenMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> {
                    RevokeTokenRequest.Builder builder = RevokeTokenRequest.newBuilder().setToken(token).setTenantName(tenantName)
                            .setRequestSignature(signed.signature()).setMessage(signed.message());
                    return operationId == null ? builder.build() : builder.setOperationId(operationId).build();
                },
                TokenServiceGrpc.TokenServiceBlockingStub::revokeToken);
    }

    public DeleteTokenResponse deleteToken(String token, String tenantName, String requestedOperationId, AviatorRequestSigner signer) {
        return executeMutation(tokenServiceBlockingStub, Constants.OP_DELETE_TOKEN,
                TokenServiceGrpc.getDeleteTokenMethod().getFullMethodName(), requestedOperationId, signer,
                (signed, operationId) -> {
                    DeleteTokenRequest.Builder builder = DeleteTokenRequest.newBuilder().setToken(token).setTenantName(tenantName)
                            .setRequestSignature(signed.signature()).setMessage(signed.message());
                    return operationId == null ? builder.build() : builder.setOperationId(operationId).build();
                },
                TokenServiceGrpc.TokenServiceBlockingStub::deleteToken);
    }

    public TokenValidationResponse validateToken(String token, String tenantName, String signature, String message) {
        TokenValidationRequest request = TokenValidationRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::validateToken, request, Constants.OP_VALIDATE_TOKEN);
    }

    public TokenValidationResponse validateUserToken(String token, String tenantName) {
        ValidateUserTokenRequest request = ValidateUserTokenRequest.newBuilder().setToken(token).setTenantName(tenantName != null ? tenantName : "").build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::validateUserToken, request, Constants.OP_VALIDATE_USER_TOKEN);
    }

    public List<Entitlement> listEntitlements(String tenantName, String signature, String message) {
        ListEntitlementsByTenantRequest request = ListEntitlementsByTenantRequest.newBuilder().setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        ListEntitlementsByTenantResponse response = GrpcUtil.executeGrpcCall(entitlementServiceBlockingStub, EntitlementServiceGrpc.EntitlementServiceBlockingStub::listEntitlementsByTenant, request, Constants.OP_LIST_ENTITLEMENTS);
        return response.getEntitlementsList();
    }

    public List<DastEntitlement> listDastEntitlements(String tenantName, String signature, String message) {
        ListDastEntitlementsByTenantRequest request = ListDastEntitlementsByTenantRequest.newBuilder().setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        ListDastEntitlementsByTenantResponse response = GrpcUtil.executeGrpcCall(dastEntitlementServiceBlockingStub, DastEntitlementServiceGrpc.DastEntitlementServiceBlockingStub::listDastEntitlementsByTenant, request, Constants.OP_LIST_DAST_ENTITLEMENTS);
        return response.getEntitlementsList();
    }

    public CorrelationServiceGrpc.CorrelationServiceStub getCorrelationAsyncStub() {
        return correlationAsyncStub;
    }

    public DastAuditServiceGrpc.DastAuditServiceStub getDastAuditAsyncStub() {
        return dastAuditAsyncStub;
    }

    public java.util.concurrent.ScheduledExecutorService getPingScheduler() {
        return pingScheduler;
    }

    public long getPingIntervalSeconds() {
        return pingIntervalSeconds;
    }

    public long getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }
}
