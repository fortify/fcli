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

import java.util.ArrayList;
import java.util.List;
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
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.Autoremediation;
import com.fortify.cli.aviator.audit.model.Change;
import com.fortify.cli.aviator.audit.model.StackTraceElement;
import com.fortify.cli.aviator.audit.model.UserPrompt;
import com.fortify.cli.aviator.util.Constants;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.AbstractBlockingStub;

class GrpcUtil {
    private static final Logger LOG = LoggerFactory.getLogger(GrpcUtil.class);

    @FunctionalInterface
    interface GrpcCall<S, T, R> {
        R call(S stub, T request) throws StatusRuntimeException;
    }

    static <S extends AbstractBlockingStub<S>, T, R> R executeGrpcCall(S stub, GrpcCall<S, T, R> call, T request, String operation) {
        try {
            S stubWithDeadline = stub.withDeadlineAfter(Constants.DEFAULT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
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
        auditResponse.setIssueId(response.getIssueId());
        auditResponse.setTier(response.getTier());
        auditResponse.setAviatorPredictionTag(response.getAviatorPredictionTag());
        auditResponse.setIsAviatorProcessed(response.getIsAviatorProcessed());
        auditResponse.setUserPrompt(response.getUserPrompt());
        auditResponse.setSystemPrompt(response.getSystemPrompt());
        return auditResponse;
    }
}