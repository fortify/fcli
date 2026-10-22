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

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.aviator.grpc.AnalysisInfo;
import com.fortify.aviator.grpc.AuditRequest;
import com.fortify.aviator.grpc.AuditorResponse;
import com.fortify.aviator.grpc.File;
import com.fortify.aviator.grpc.Fragment;
import com.fortify.aviator.grpc.IssueData;
import com.fortify.aviator.grpc.StackTraceElementList;
import com.fortify.cli.aviator._common.exception.AviatorConnectivityException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditResponse.AuditSkipReason;
import com.fortify.cli.aviator.audit.model.Autoremediation;
import com.fortify.cli.aviator.audit.model.Change;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.fpr.utils.SourceCodeEnricher;
import com.fortify.cli.aviator.util.Constants;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;

class GrpcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcUtil.class);
    SourceCodeEnricher sourceCodeEnricher;

    @FunctionalInterface
    interface GrpcCall<S, T, R> {
        R call(S stub, T request) throws StatusRuntimeException;
    }

    // gRPC 1.76 stubs have no withoutWaitForReady(); this interceptor clears the flag per call.
    private static final ClientInterceptor WITHOUT_WAIT_FOR_READY = new ClientInterceptor() {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                CallOptions callOptions, Channel next) {
            return next.newCall(method, callOptions.withoutWaitForReady());
        }
    };

    static <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation) {
        return executeGrpcCall(stub, call, request, operation, AviatorRequestPolicy.UNCHANGED, null);
    }

    /**
     * Executes a unary call with the default deadline. The policy selects wait-for-ready and
     * connectivity failure handling; the context must be non-null if the policy classifies failures.
     */
    static <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation,
            AviatorRequestPolicy policy, AviatorConnectionContext context) {
        try {
            return call.call(configureStub(stub, policy), request);
        } catch (StatusRuntimeException e) {
            throw toConnectivityException(e, operation, policy, context)
                .orElseGet(() -> toAviatorException(e, operation));
        } catch (Exception e) {
            String errorMessage = "An unexpected client-side error occurred during the " + operation + " operation.";
            LOG.error(errorMessage, e);
            throw new AviatorTechnicalException(errorMessage, e);
        }
    }

    private static <S extends AbstractBlockingStub<S>> S configureStub(S stub, AviatorRequestPolicy policy) {
        var stubWithDeadline = stub.withDeadlineAfter(Constants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        return policy.isWaitForReady() ? stubWithDeadline : stubWithDeadline.withInterceptors(WITHOUT_WAIT_FOR_READY);
    }

    private static Optional<RuntimeException> toConnectivityException(StatusRuntimeException e, String operation,
            AviatorRequestPolicy policy, AviatorConnectionContext context) {
        if ( !policy.isClassifyConnectivityFailures() ) {
            return Optional.empty();
        }
        return AviatorConnectivityFailureClassifier.classify(e, context)
            .map(kind -> newConnectivityException(kind, operation, e, context));
    }

    private static AviatorConnectivityException newConnectivityException(AviatorConnectivityFailureKind kind, String operation,
            StatusRuntimeException e, AviatorConnectionContext context) {
        return new AviatorConnectivityException(kind.formatMessage(context), kind.name(), operation,
            e.getStatus().getCode().name(), context.route(), context.firstHop().display(), e);
    }

    private static RuntimeException toAviatorException(StatusRuntimeException e, String operation) {
        Status status = e.getStatus();
        String serverDescription = status.getDescription() != null && !status.getDescription().isBlank()
                ? status.getDescription()
                : "No additional details were provided by the server.";

        switch (status.getCode()) {
            case INVALID_ARGUMENT:
                return new AviatorSimpleException(String.format("Invalid input for %s. The server reported: %s. Please check the provided arguments.", operation, serverDescription));
            case NOT_FOUND:
                return new AviatorSimpleException(String.format("The requested resource was not found during %s. The server reported: %s. Please verify the name or ID is correct.", operation, serverDescription));
            case ALREADY_EXISTS:
                return new AviatorSimpleException(String.format("Cannot perform %s because a resource with the same identifier already exists. The server reported: %s.", operation, serverDescription));
            case FAILED_PRECONDITION:
                return new AviatorSimpleException(String.format("The %s operation could not be completed because a required condition was not met. The server reported: %s.", operation, serverDescription));

            case PERMISSION_DENIED:
                if (serverDescription.toLowerCase().contains("invalid signature")) {
                    return new AviatorSimpleException("Permission Denied: Invalid signature. Please verify the private key in your admin configuration is correct and corresponds to the public key registered with Aviator.");
                } else {
                    return new AviatorSimpleException(String.format("Permission Denied for %s. You may not have the required roles for this action. Server details: %s", operation, serverDescription));
                }
            case UNAUTHENTICATED:
                return new AviatorSimpleException("Authentication Failed: The token or credentials used are invalid or expired. Please log in again using 'fcli aviator session login' or verify your admin configuration.");

            case RESOURCE_EXHAUSTED:
                return new AviatorSimpleException(String.format("The server's resource limits were exceeded during %s. Please try again later or contact support. Server details: %s", operation, serverDescription));
            case DEADLINE_EXCEEDED:
                String timeoutMessage = String.format(
                        "The %s operation timed out because the server did not respond in time.\n\n" +
                                "Please check the following:\n" +
                                "  1. The Aviator URL in your configuration is correct and reachable.\n" +
                                "  2. Your network connection is stable and any firewalls or proxies are properly configured.\n\n" +
                                "If the URL and network are correct, the server may be experiencing high load. Please try again later.",
                        operation
                );
                return new AviatorSimpleException(timeoutMessage);

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
                return new AviatorTechnicalException(techMessage, e);
        }
    }

    static AuditRequest convertToAuditRequest(UserPrompt userPrompt, String streamId, String requestId) {
        List<StackTraceElementList> stackTraceElementLists = new ArrayList<>();
        if (userPrompt.getStackTrace() != null) {
            for (List<StackTraceElement> innerList : userPrompt.getStackTrace()) {
                StackTraceElementList stackTraceElementList = StackTraceElementList.newBuilder().addAllElements(innerList.stream().map(GrpcUtil::convertToStackTraceElement).collect(Collectors.toList())).build();
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
            builder.addAllFirstStackTrace(userPrompt.getFirstStackTrace().stream().map(GrpcUtil::convertToStackTraceElement).collect(Collectors.toList()));
        }
        if (userPrompt.getLongestStackTrace() != null) {
            builder.addAllLongestStackTrace(userPrompt.getLongestStackTrace().stream().map(GrpcUtil::convertToStackTraceElement).collect(Collectors.toList()));
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
    private static com.fortify.aviator.grpc.StackTraceElement convertToStackTraceElement(StackTraceElement element) {
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
            builder.addAllInnerStackTrace(element.getInnerStackTrace().stream().map(GrpcUtil::convertToStackTraceElement).collect(Collectors.toList()));
        }
        return builder.build();
    }

    static AuditResponse convertToAuditResponse(AuditorResponse response) {
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
        if ("SKIPPED".equalsIgnoreCase(response.getStatus())) {
            auditResponse.setAuditSkipReason(AuditSkipReason.SKIPPED_BY_AVIATOR);
        }
        auditResponse.setIssueId(response.getIssueId());
        auditResponse.setSubmittedToAviator(true);
        auditResponse.setTier(response.getTier());
        auditResponse.setAviatorPredictionTag(response.getAviatorPredictionTag());
        auditResponse.setIsAviatorProcessed(response.getIsAviatorProcessed());
        auditResponse.setUserPrompt(response.getUserPrompt());
        auditResponse.setSystemPrompt(response.getSystemPrompt());
        return auditResponse;
    }
}
