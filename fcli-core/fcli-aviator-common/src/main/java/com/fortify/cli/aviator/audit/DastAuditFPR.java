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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.grpc.DastAuditStreamConfig;
import com.fortify.cli.aviator.grpc.DastAuditStreamResult;
import com.fortify.cli.aviator.grpc.DastAuditWorkItem;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Coordinates parsing, filtering, server auditing, and DAST audit.xml updates.
 */
public final class DastAuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(DastAuditFPR.class);

    private DastAuditFPR() {}

    private record EligibilityResult(
            List<DastAuditWorkItem> workItems,
            int missingId,
            int duplicate,
            int suppressed,
            int processed) {}

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
                + "(missingId={}, duplicate={}, suppressed={}, alreadyProcessed={})",
            totalReported, workItems.size(), locallySkipped, eligibility.missingId(),
            eligibility.duplicate(), eligibility.suppressed(), eligibility.processed());

        if (workItems.isEmpty()) {
            LOG.info("DAST audit skipped because no eligible findings remain");
            return emptyResult(totalReported, locallySkipped);
        }

        DastAuditStreamResult streamResult = streamRunner.run(config, workItems, totalReported).join();
        Map<String, AuditResponse> successfulResponses = new LinkedHashMap<>();
        int truePositives = 0;
        int falsePositivesSuppressed = 0;
        int likelyFalsePositives = 0;
        int failed = 0;
        int serverSkipped = 0;
        Set<String> respondedIssueIds = new java.util.HashSet<>();

        for (var result : streamResult.results()) {
            respondedIssueIds.add(result.issueId());
            AuditResponse response = DastAuditDecisionMapper.toAuditResponse(result);
            if ("SUCCESS".equalsIgnoreCase(response.getStatus()) && response.getAuditResult() != null) {
                successfulResponses.put(result.issueId(), response);
                var success = (com.fortify.cli.aviator.grpc.DastAuditResult.Success) result;
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
        int missingResponses = 0;
        for (DastAuditWorkItem workItem : workItems) {
            if (!respondedIssueIds.contains(workItem.issue().getId())) {
                missingResponses++;
                LOG.warn("DAST issue {} received no terminal server response", workItem.issue().getId());
            }
        }
        failed += missingResponses;

        var updatedFile = successfulResponses.isEmpty()
            ? null
            : auditProcessor.updateAndSaveDastAuditXml(successfulResponses, tagMappingConfig);
        int succeeded = successfulResponses.size();
        LOG.info("DAST audit responses: submitted={}, succeeded={}, serverSkipped={}, failed={}, missingResponses={}",
            workItems.size(), succeeded, serverSkipped, failed, missingResponses);
        String status = succeeded == workItems.size() ? "AUDITED"
            : succeeded > 0 ? "PARTIALLY_AUDITED" : "FAILED";
        String message = succeeded == 0 ? "No DAST audit responses were successfully processed" : null;
        return new DastAuditFprResult(
            updatedFile, status, message, totalReported, workItems.size(), workItems.size(), succeeded,
            truePositives, falsePositivesSuppressed, likelyFalsePositives,
            locallySkipped + serverSkipped, failed,
            streamResult.reservedQuota(), streamResult.exceededCount(), streamResult.unlimitedQuota(),
            streamResult.quotaLastUpdated(), streamResult.nextQuotaUpdateMessage());
    }

    private static boolean isSuppressedFalsePositive(AuditResponse response, TagMappingConfig tagMappingConfig) {
        boolean tierOne = "GOLD".equalsIgnoreCase(response.getTier());
        return Boolean.TRUE.equals(tagMappingConfig.getResult(
            tierOne, TagMappingConfig.ResultType.FP).getSuppress());
    }

    private static EligibilityResult eligibleWorkItems(
            List<com.fortify.cli.aviator.dast.DastSession> sessions,
            Map<String, AuditIssue> auditIssues) {
        var workItems = new ArrayList<DastAuditWorkItem>();
        var seenIssueIds = new java.util.HashSet<String>();
        int missingId = 0;
        int duplicate = 0;
        int suppressed = 0;
        int processed = 0;
        for (var session : sessions) {
            for (var issue : session.getIssues()) {
                String issueId = issue.getId();
                if (issueId == null || issueId.isBlank()) {
                    missingId++;
                    LOG.debug("Skipping DAST finding without an issue ID in session {}", session.getRequestId());
                    continue;
                }
                if (!seenIssueIds.add(issueId)) {
                    duplicate++;
                    LOG.debug("Skipping duplicate DAST issue {} in session {}", issueId, session.getRequestId());
                    continue;
                }
                AuditIssue auditIssue = auditIssues.get(issueId);
                if (auditIssue != null && auditIssue.isSuppressed()) {
                    suppressed++;
                    LOG.debug("Skipping DAST issue {} because it is already suppressed", issueId);
                    continue;
                }
                if (auditIssue != null && isProcessedByAviator(auditIssue)) {
                    processed++;
                    LOG.debug("Skipping DAST issue {} because it is already processed by Aviator", issueId);
                    continue;
                }
                workItems.add(new DastAuditWorkItem(session, issue));
            }
        }
        return new EligibilityResult(List.copyOf(workItems), missingId, duplicate, suppressed, processed);
    }

    private static boolean isProcessedByAviator(AuditIssue auditIssue) {
        return Constants.PROCESSED_BY_AVIATOR.equalsIgnoreCase(
            auditIssue.getTags().get(Constants.AVIATOR_STATUS_TAG_ID));
    }

    private static DastAuditFprResult emptyResult(int totalReported, int skipped) {
        return new DastAuditFprResult(
            null, "SKIPPED", "No eligible DAST findings to audit", totalReported, 0, 0, 0,
            0, 0, 0, skipped, 0, 0, 0, false, null, null);
    }
}