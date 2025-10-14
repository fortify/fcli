/*
 * Copyright 2021-2025 Open Text.
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

import java.util.List;
import java.util.Map;
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
import com.fortify.aviator.grpc.AuditorServiceGrpc;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.util.Constants;
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

public class AviatorGrpcClient implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorGrpcClient.class);

    private final IAviatorLogger logger;
    private final ManagedChannel channel;
    private final AuditorServiceGrpc.AuditorServiceStub asyncStub;
    private final ApplicationServiceGrpc.ApplicationServiceBlockingStub blockingStub;
    private final TokenServiceGrpc.TokenServiceBlockingStub tokenServiceBlockingStub;
    private final EntitlementServiceGrpc.EntitlementServiceBlockingStub entitlementServiceBlockingStub;
    private final long defaultTimeoutSeconds;
    private final java.util.concurrent.ExecutorService processingExecutor;
    private final long pingIntervalSeconds;
    private final java.util.concurrent.ScheduledExecutorService pingScheduler;
    final AtomicBoolean isShutdown = new AtomicBoolean(false);

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger, long pingIntervalSeconds) {
        LOG.info("Initializing AviatorGrpcClient with ManagedChannel");
        this.logger = logger;
        this.channel = channel;
        this.asyncStub = AuditorServiceGrpc.newStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.blockingStub = ApplicationServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.tokenServiceBlockingStub = TokenServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
        this.entitlementServiceBlockingStub = EntitlementServiceGrpc.newBlockingStub(channel).withCompression("gzip").withMaxInboundMessageSize(Constants.MAX_MESSAGE_SIZE).withMaxOutboundMessageSize(Constants.MAX_MESSAGE_SIZE).withWaitForReady();
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
        LOG.info("Initialized AviatorGrpcClient - Host: {}, Port: {}", host, port);
    }

    public AviatorGrpcClient(ManagedChannel channel, long defaultTimeoutSeconds, IAviatorLogger logger) {
        this(channel, defaultTimeoutSeconds, logger, 30);
    }

    public CompletableFuture<Map<String, AuditResponse>> processBatchRequests(Queue<UserPrompt> requests, String projectName, String FPRBuildId, String SSCApplicationName, String SSCApplicationVersion, String token) {
        AviatorStreamProcessor processor = new AviatorStreamProcessor(this, logger, asyncStub, processingExecutor, pingScheduler, pingIntervalSeconds, defaultTimeoutSeconds);
        CompletableFuture<Map<String, AuditResponse>> future = processor.processBatchRequests(requests, projectName, FPRBuildId, SSCApplicationName, SSCApplicationVersion, token);
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

        LOG.info("Client closed");
    }

    public Application createApplication(String name, String tenantName, String signature, String message) {
        CreateApplicationRequest request = CreateApplicationRequest.newBuilder().setName(name).setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::createApplication, request, Constants.OP_CREATE_APP);
    }

    public Application updateApplication(String projectId, String newName, String signature, String message, String tenantName) {
        UpdateApplicationRequest request = UpdateApplicationRequest.newBuilder().setId(Long.parseLong(projectId)).setName(newName).setTenantName(tenantName).setSignature(signature).setMessage(message).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::updateApplication, request, Constants.OP_UPDATE_APP);
    }

    public ApplicationResponseMessage deleteApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::deleteApplication, request, Constants.OP_DELETE_APP);
    }

    public Application getApplication(String projectId, String signature, String message, String tenantName) {
        ApplicationById request = ApplicationById.newBuilder().setId(Long.parseLong(projectId)).setSignature(signature).setMessage(message).setTenantName(tenantName).build();
        return GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::getApplication, request, Constants.OP_GET_APP);
    }

    public List<Application> listApplication(String tenantName, String signature, String message) {
        ApplicationByTenantName request = ApplicationByTenantName.newBuilder().setName(tenantName).setSignature(signature).setMessage(message).build();
        ApplicationList applicationList = GrpcUtil.executeGrpcCall(blockingStub, ApplicationServiceGrpc.ApplicationServiceBlockingStub::listApplications, request, Constants.OP_LIST_APPS);
        return applicationList.getApplicationsList();
    }

    public TokenGenerationResponse generateToken(String email, String tokenName, String signature, String message, String tenantName, String endDate) {
        TokenGenerationRequest request = TokenGenerationRequest.newBuilder().setEmail(email != null ? email : "").setCustomTokenName(tokenName != null ? tokenName : "").setRequestSignature(signature).setEndDate(com.fortify.cli.aviator.util.StringUtil.isEmpty(endDate) ? "" : endDate).setMessage(message).setTenantName(tenantName).build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::generateToken, request, Constants.OP_GENERATE_TOKEN);
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

    public RevokeTokenResponse revokeToken(String token, String email, String tenantName, String signature, String message) {
        RevokeTokenRequest request = RevokeTokenRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::revokeToken, request, Constants.OP_REVOKE_TOKEN);
    }

    public DeleteTokenResponse deleteToken(String token, String email, String tenantName, String signature, String message) {
        DeleteTokenRequest request = DeleteTokenRequest.newBuilder().setToken(token).setTenantName(tenantName).setRequestSignature(signature).setMessage(message).build();
        return GrpcUtil.executeGrpcCall(tokenServiceBlockingStub, TokenServiceGrpc.TokenServiceBlockingStub::deleteToken, request, Constants.OP_DELETE_TOKEN);
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
}