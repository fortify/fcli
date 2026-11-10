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
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
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
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.rpc.ErrorInfo;
import com.google.rpc.RetryInfo;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.AbstractBlockingStub;

class GrpcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcUtil.class);
    SourceCodeEnricher sourceCodeEnricher;

    @FunctionalInterface
    interface GrpcCall<S, T, R> {
        R call(S stub, T request) throws StatusRuntimeException;
    }

    static <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation) {
        return executeGrpcCall(stub, call, request, operation, null);
    }

    /** @param unimplementedHint optional extra advice appended to the error message when the server does not implement the call */
    static <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation, String unimplementedHint) {
        try {
            S stubWithDeadline = stub.withDeadlineAfter(Constants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return call.call(stubWithDeadline, request);
        } catch (StatusRuntimeException e) {
            throw toAviatorException(e, operation, unimplementedHint);
        } catch (Exception e) {
            String errorMessage = "An unexpected client-side error occurred during the " + operation + " operation.";
            LOG.error(errorMessage, e);
            throw new AviatorTechnicalException(errorMessage, e);
        }
    }

    /** Maps a failed call to the user facing exception types. */
    static RuntimeException toAviatorException(StatusRuntimeException e, String operation, String unimplementedHint) {
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

            case UNIMPLEMENTED:
                LOG.warn("gRPC call for '{}' is not implemented by the server: {}", operation, serverDescription);
                return new AviatorSimpleException(String.format(
                        "The Aviator service does not support the %s operation.%s",
                        operation, unimplementedHint == null ? "" : " " + unimplementedHint));

            case CANCELLED:
            case UNKNOWN:
            case ABORTED:
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

    /** Maximum attempts (initial call plus retries) of one mutation; every attempt reuses the operation id. */
    static final int MAX_MUTATION_ATTEMPTS = 4;
    private static final String OPERATION_ERROR_DOMAIN = "aviator.operation";

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }

    static <S extends AbstractBlockingStub<S>, T, R> R executeMutationCall(S stub, GrpcCall<S, T, R> call,
            Supplier<T> requests, String operation, String operationId) {
        return executeMutationCall(stub, call, requests, operation, operationId, Thread::sleep);
    }

    /**
     * Executes a state-changing call. When an operation id is in use, outcomes that may or may not have
     * committed (deadline, unavailable, aborted/in progress) are retried with the <em>same</em> id; each attempt
     * is re-signed through {@code requests} because the server re-checks signature and freshness every time.
     * Without an operation id (legacy server) a mutation is never retried, because a retry could apply it twice.
     */
    static <S extends AbstractBlockingStub<S>, T, R> R executeMutationCall(S stub, GrpcCall<S, T, R> call,
            Supplier<T> requests, String operation, String operationId, Sleeper sleeper) {
        for (int attempt = 1;; attempt++) {
            T request = requests.get();
            try {
                return call.call(stub.withDeadlineAfter(Constants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS), request);
            } catch (StatusRuntimeException e) {
                if (operationId != null && attempt < MAX_MUTATION_ATTEMPTS && isSafeToRetry(e)) {
                    long delay = retryDelayMillis(e, attempt);
                    LOG.debug("Retrying {} with the same operation id after status {} (attempt {}/{}, delay {} ms)",
                            operation, e.getStatus().getCode(), attempt + 1, MAX_MUTATION_ATTEMPTS, delay);
                    try {
                        sleeper.sleep(delay);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new AviatorTechnicalException("Interrupted while retrying " + operation
                                + unknownOutcomeHint(operationId, operation), interrupted);
                    }
                    continue;
                }
                throw toMutationException(e, stub, operation, operationId);
            } catch (RuntimeException e) {
                String errorMessage = "An unexpected client-side error occurred during the " + operation + " operation.";
                LOG.error(errorMessage, e);
                throw new AviatorTechnicalException(errorMessage, e);
            }
        }
    }

    private static boolean isSafeToRetry(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case DEADLINE_EXCEEDED, UNAVAILABLE, ABORTED, UNKNOWN -> true;
            default -> false;
        };
    }

    private static boolean isUnknownOutcome(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case DEADLINE_EXCEEDED, UNAVAILABLE, ABORTED, UNKNOWN, CANCELLED, INTERNAL, DATA_LOSS -> true;
            default -> false;
        };
    }

    private static long retryDelayMillis(StatusRuntimeException e, int attempt) {
        long serverHint = retryDelayHintMillis(e);
        long backoff = Math.min(4000L, 500L << (attempt - 1));
        long delay = serverHint > 0 ? serverHint : backoff;
        return Math.max(200L, Math.min(5000L, delay)) + ThreadLocalRandom.current().nextLong(0, 150);
    }

    private static long retryDelayHintMillis(StatusRuntimeException e) {
        com.google.rpc.Status status = StatusProto.fromThrowable(e);
        if (status == null) {
            return 0;
        }
        for (Any detail : status.getDetailsList()) {
            if (detail.is(RetryInfo.class)) {
                try {
                    var delay = detail.unpack(RetryInfo.class).getRetryDelay();
                    return delay.getSeconds() * 1000 + delay.getNanos() / 1_000_000;
                } catch (InvalidProtocolBufferException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    /** Reason from the server's {@code aviator.operation} ErrorInfo, or {@code null} for any other failure. */
    static String operationReason(Throwable failure) {
        com.google.rpc.Status status = StatusProto.fromThrowable(failure);
        if (status == null) {
            return null;
        }
        for (Any detail : status.getDetailsList()) {
            if (detail.is(ErrorInfo.class)) {
                try {
                    ErrorInfo info = detail.unpack(ErrorInfo.class);
                    if (OPERATION_ERROR_DOMAIN.equals(info.getDomain())) {
                        return info.getReason();
                    }
                } catch (InvalidProtocolBufferException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    static String unknownOutcomeHint(String operationId) {
        return unknownOutcomeHint(operationId, null);
    }

    static String unknownOutcomeHint(String operationId, String operation) {
        if (Constants.OP_GENERATE_TOKEN.equals(operation)) {
            return "\nThe outcome of this operation is unknown: the server may already have created the token. A created token "
                    + "cannot be shown again. Re-run the same command with --operation-id " + operationId + " to find out: the "
                    + "token is created only if the first attempt did not create it. If the server reports that the operation was "
                    + "already completed, run 'fcli aviator token list', revoke the affected token and create a new one. "
                    + "Operation IDs are honored for " + AviatorOperationId.DEFAULT_MAX_AGE.toHours() + " hours.";
        }
        return "\nThe outcome of this operation is unknown: the server may already have applied it. Do not start a new "
                + "operation. Re-run the same command with --operation-id " + operationId + " to retry safely: the server "
                + "applies the operation at most once and returns the original result if it was already applied. "
                + "Operation IDs are honored for " + AviatorOperationId.DEFAULT_MAX_AGE.toHours() + " hours.";
    }

    private static String alreadyCompletedMessage(String operation, String description) {
        if (Constants.OP_GENERATE_TOKEN.equals(operation)) {
            return "The token was already created by an earlier attempt with this operation ID, and a created token cannot be "
                    + "shown again. Run 'fcli aviator token list', revoke the affected token and create a new one.";
        }
        if (description != null && !description.isBlank()) {
            return description;
        }
        return String.format("The %s operation was already completed by an earlier attempt with this operation ID. "
                + "Check the current state with the corresponding list or get command.", operation);
    }

    private static <S extends AbstractBlockingStub<S>> RuntimeException toMutationException(StatusRuntimeException e, S stub,
            String operation, String operationId) {
        String reason = operationReason(e);
        String description = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : "";
        if (reason != null) {
            return switch (reason) {
                case "OPERATION_IN_PROGRESS" -> new AviatorSimpleException(String.format(
                        "The %s operation is still being processed by the server. Wait a moment, then re-run the same command with --operation-id %s.",
                        operation, operationId));
                case "OPERATION_ID_EXPIRED" -> new AviatorSimpleException(String.format(
                        "Operation ID %s is older than %d hours and can no longer be matched to a server record, so the %s operation was not applied again. "
                                + "Check the current state with the corresponding list or get command before running the command again with a new operation ID.",
                        operationId, AviatorOperationId.DEFAULT_MAX_AGE.toHours(), operation));
                case "OPERATION_ID_REUSED" -> new AviatorSimpleException(String.format(
                        "Operation ID %s was already used for a different request. Use a new operation ID for a new request.",
                        operationId));
                case "OPERATION_ALREADY_COMPLETED" -> new AviatorSimpleException(alreadyCompletedMessage(operation, description));
                default -> new AviatorSimpleException(String.format(
                        "The server refused the %s request (%s: %s). This usually means fcli and the Aviator server are not on compatible versions.",
                        operation, reason, description));
            };
        }
        RuntimeException mapped = toAviatorException(e, operation, null);
        if (operationId == null || !isUnknownOutcome(e)) {
            return mapped;
        }
        String hint = unknownOutcomeHint(operationId, operation);
        if (mapped instanceof AviatorSimpleException) {
            return new AviatorSimpleException(mapped.getMessage() + hint, mapped.getCause());
        }
        if (mapped instanceof AviatorTechnicalException) {
            return new AviatorTechnicalException(mapped.getMessage() + hint, mapped.getCause());
        }
        return mapped;
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
