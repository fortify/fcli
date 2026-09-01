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
package com.fortify.cli.aviator.audit;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.AuditTier;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.dast.DastSession;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.grpc.DastAuditResult;
import com.fortify.cli.aviator.grpc.DastAuditStreamConfig;
import com.fortify.cli.aviator.grpc.DastAuditStreamResult;
import com.fortify.cli.aviator.grpc.DastAuditWorkItem;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

import lombok.Builder;

/**
 * Coordinates parsing, filtering, server auditing, and DAST audit.xml updates.
 */
public final class DastAuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(DastAuditFPR.class);

    private DastAuditFPR() {}

    @Builder
    private record EligibilityResult(
            List<DastAuditWorkItem> workItems,
            int missingId,
            int duplicate,
            int suppressed,
            int alreadyProcessed,
            int humanAudited) {
        private static class EligibilityResultBuilder {
            private List<DastAuditWorkItem> workItems = new ArrayList<>();

            private EligibilityResultBuilder addWorkItem(DastAuditWorkItem workItem) {
                workItems.add(workItem);
                return this;
            }

            private EligibilityResultBuilder incrementMissingId() {
                missingId++;
                return this;
            }

            private EligibilityResultBuilder incrementDuplicate() {
                duplicate++;
                return this;
            }

            private EligibilityResultBuilder incrementSuppressed() {
                suppressed++;
                return this;
            }

            private EligibilityResultBuilder incrementAlreadyProcessed() {
                alreadyProcessed++;
                return this;
            }

            private EligibilityResultBuilder incrementHumanAudited() {
                humanAudited++;
                return this;
            }
        }
    }

    private record ResponseSummary(
            Map<String, AuditResponse> successfulResponses,
            int truePositives,
            int falsePositivesSuppressed,
            int likelyFalsePositives,
            int serverSkipped,
            int failed,
            int missingResponses) {}

    @FunctionalInterface
    public interface StreamRunner {
        CompletableFuture<DastAuditStreamResult> run(
            DastAuditStreamConfig config, List<DastAuditWorkItem> workItems, int totalReportedIssues);
    }

    public static DastAuditFprResult audit(
            FprHandle fprHandle,
            DastAuditStreamConfig config,
            TagMappingConfig tagMappingConfig,
            StreamRunner streamRunner) {
        tagMappingConfig.validateForDast();
        var auditProcessor = new AuditProcessor(fprHandle);
        Map<String, AuditIssue> auditIssues = auditProcessor.processAuditXML();
        var sessions = new StreamingWebInspectParser(fprHandle).parseSessions();
        EligibilityResult eligibility = eligibleWorkItems(sessions, auditIssues);
        List<DastAuditWorkItem> workItems = eligibility.workItems();
        int totalReported = sessions.stream().mapToInt(session -> session.getIssues().size()).sum();
        int locallySkipped = totalReported - workItems.size();
        LOG.info("DAST audit eligibility: reported={}, eligible={}, skipped={} "
                + "(missingId={}, duplicate={}, suppressed={}, alreadyProcessed={}, humanAudited={})",
            totalReported, workItems.size(), locallySkipped, eligibility.missingId(),
            eligibility.duplicate(), eligibility.suppressed(), eligibility.alreadyProcessed(),
            eligibility.humanAudited());

        if (workItems.isEmpty()) {
            LOG.info("DAST audit skipped because no eligible findings remain");
            return emptyResult(totalReported, locallySkipped);
        }

        CompletableFuture<DastAuditStreamResult> streamFuture = streamRunner.run(config, workItems, totalReported);
        if (streamFuture == null) {
            throw new AviatorTechnicalException("DAST audit stream did not return a completion future");
        }
        DastAuditStreamResult streamResult = streamFuture.join();
        if (streamResult == null) {
            throw new AviatorTechnicalException("DAST audit stream completed without a result");
        }
        ResponseSummary summary = summarizeResponses(streamResult, workItems, tagMappingConfig);
        var updatedFile = summary.successfulResponses().isEmpty()
            ? null
            : auditProcessor.updateAndSaveDastAuditXml(summary.successfulResponses(), tagMappingConfig);
        return buildResult(streamResult, summary, updatedFile, totalReported, locallySkipped, workItems.size());
    }

    private static ResponseSummary summarizeResponses(
            DastAuditStreamResult streamResult,
            List<DastAuditWorkItem> workItems,
            TagMappingConfig tagMappingConfig) {
        Map<String, AuditResponse> successfulResponses = new LinkedHashMap<>();
        int truePositives = 0;
        int falsePositivesSuppressed = 0;
        int likelyFalsePositives = 0;
        int failed = 0;
        int serverSkipped = 0;
        Set<String> respondedIssueIds = new HashSet<>();

        for (var result : streamResult.results()) {
            respondedIssueIds.add(result.issueId());
            AuditResponse response = DastAuditDecisionMapper.toAuditResponse(result);
            if ("SUCCESS".equalsIgnoreCase(response.getStatus()) && response.getAuditResult() != null) {
                successfulResponses.put(result.issueId(), response);
                var success = (DastAuditResult.Success) result;
                LOG.debug("DAST issue {} audited successfully: confidence={}, tier={}, result={}",
                    result.issueId(), success.confidence(), response.getTier(), response.getAuditResult().getTagValue());
                if (Constants.EXPLOITABLE.equals(response.getAuditResult().getTagValue())) {
                    truePositives++;
                } else if (isSuppressedFalsePositive(response, tagMappingConfig)) {
                    falsePositivesSuppressed++;
                } else {
                    likelyFalsePositives++;
                }
            } else if ("SKIPPED".equalsIgnoreCase(result.status())) {
                serverSkipped++;
                LOG.debug("DAST issue {} skipped by server: statusMessage={}",
                    result.issueId(), result.statusMessage());
            } else {
                failed++;
                LOG.warn("DAST issue {} failed: status={}, statusMessage={}",
                    result.issueId(), result.status(), result.statusMessage());
            }
        }
        int missingResponses = countMissingResponses(workItems, respondedIssueIds);
        return new ResponseSummary(successfulResponses, truePositives, falsePositivesSuppressed,
            likelyFalsePositives, serverSkipped, failed + missingResponses, missingResponses);
    }

    private static int countMissingResponses(
            List<DastAuditWorkItem> workItems,
            Set<String> respondedIssueIds) {
        int missingResponses = 0;
        for (DastAuditWorkItem workItem : workItems) {
            if (!respondedIssueIds.contains(workItem.issue().getId())) {
                missingResponses++;
                LOG.warn("DAST issue {} received no terminal server response", workItem.issue().getId());
            }
        }
        return missingResponses;
    }

    private static DastAuditFprResult buildResult(
            DastAuditStreamResult streamResult,
            ResponseSummary summary,
            File updatedFile,
            int totalReported,
            int locallySkipped,
            int submitted) {
        int succeeded = summary.successfulResponses().size();
        LOG.info("DAST audit responses: submitted={}, succeeded={}, serverSkipped={}, failed={}, missingResponses={}",
            submitted, succeeded, summary.serverSkipped(), summary.failed(), summary.missingResponses());
        DastAuditFprStatus status = succeeded == submitted ? DastAuditFprStatus.AUDITED
            : succeeded > 0 ? DastAuditFprStatus.PARTIALLY_AUDITED : DastAuditFprStatus.FAILED;
        String message = succeeded == 0 ? "No DAST audit responses were successfully processed" : null;
        return DastAuditFprResult.builder()
            .updatedFile(updatedFile)
            .status(status)
            .message(message)
            .totalReported(totalReported)
            .eligible(submitted)
            .submitted(submitted)
            .succeeded(succeeded)
            .truePositives(summary.truePositives())
            .falsePositivesSuppressed(summary.falsePositivesSuppressed())
            .likelyFalsePositives(summary.likelyFalsePositives())
            .skipped(locallySkipped + summary.serverSkipped())
            .failed(summary.failed())
            .reservedQuota(streamResult.reservedQuota())
            .exceededCount(streamResult.exceededCount())
            .unlimitedQuota(streamResult.unlimitedQuota())
            .quotaLastUpdated(streamResult.quotaLastUpdated())
            .nextQuotaUpdateMessage(streamResult.nextQuotaUpdateMessage())
            .build();
    }

    private static boolean isSuppressedFalsePositive(AuditResponse response, TagMappingConfig tagMappingConfig) {
        boolean tierOne = AuditTier.fromServerValue(response.getTier()) == AuditTier.GOLD;
        return Boolean.TRUE.equals(tagMappingConfig.getResult(
            tierOne, TagMappingConfig.ResultType.FP).getSuppress());
    }

    private static EligibilityResult eligibleWorkItems(
            List<DastSession> sessions,
            Map<String, AuditIssue> auditIssues) {
        var result = EligibilityResult.builder();
        var seenIssueIds = new HashSet<String>();
        for (var session : sessions) {
            for (var issue : session.getIssues()) {
                String issueId = issue.getId();
                if (issueId == null || issueId.isBlank()) {
                    result.incrementMissingId();
                    LOG.debug("Skipping DAST finding without an issue ID in session {}", session.getRequestId());
                    continue;
                }
                if (!seenIssueIds.add(issueId)) {
                    result.incrementDuplicate();
                    LOG.debug("Skipping duplicate DAST issue {} in session {}", issueId, session.getRequestId());
                    continue;
                }
                AuditIssue auditIssue = auditIssues.get(issueId);
                if (auditIssue != null && auditIssue.isSuppressed()) {
                    result.incrementSuppressed();
                    LOG.debug("Skipping DAST issue {} because it is already suppressed", issueId);
                    continue;
                }
                if (auditIssue != null && isProcessedByAviator(auditIssue)) {
                    result.incrementAlreadyProcessed();
                    LOG.debug("Skipping DAST issue {} because it is already processed by Aviator", issueId);
                    continue;
                }
                if (auditIssue != null && isHumanAudited(auditIssue)) {
                    result.incrementHumanAudited();
                    LOG.debug("Skipping DAST issue {} because it is already audited by a human", issueId);
                    continue;
                }
                result.addWorkItem(new DastAuditWorkItem(session, issue));
            }
        }
        return result.build();
    }

    private static boolean isProcessedByAviator(AuditIssue auditIssue) {
        Map<String, String> tags = getTags(auditIssue);
        return Constants.PROCESSED_BY_AVIATOR.equalsIgnoreCase(tags.get(Constants.AVIATOR_STATUS_TAG_ID))
            || tags.containsKey(Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID);
    }

    private static boolean isHumanAudited(AuditIssue auditIssue) {
        Map<String, String> tags = getTags(auditIssue);
        return isAuditDecision(tags.get(Constants.AUDITOR_STATUS_TAG_ID))
            || isAuditDecision(tags.get(Constants.FOD_TAG_ID))
            || isAnalysisDecision(tags.get(Constants.ANALYSIS_TAG_ID));
    }

    private static Map<String, String> getTags(AuditIssue auditIssue) {
        return auditIssue.getTags() == null ? Map.of() : auditIssue.getTags();
    }

    private static boolean isAuditDecision(String value) {
        return value != null && !value.isBlank()
            && !Constants.PENDING_REVIEW.equalsIgnoreCase(value)
            && !"Pending Review".equalsIgnoreCase(value);
    }

    private static boolean isAnalysisDecision(String value) {
        return isAuditDecision(value) && !"Not Set".equalsIgnoreCase(value);
    }

    private static DastAuditFprResult emptyResult(int totalReported, int skipped) {
        return DastAuditFprResult.builder()
            .status(DastAuditFprStatus.SKIPPED)
            .message("No eligible DAST findings to audit")
            .totalReported(totalReported)
            .skipped(skipped)
            .build();
    }
}