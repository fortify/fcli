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
package com.fortify.cli.aviator.fpr;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterTemplate;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FilterTemplateParser;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.StringUtil;

import lombok.Getter;

public class FPRProcessor {

    private static final Logger logger = LoggerFactory.getLogger(FPRProcessor.class);
    private final FprHandle fprHandle;
    private final Map<String, AuditIssue> auditIssueMap;
    private final AuditProcessor auditProcessor;

    @Getter
    private FPRInfo fprInfo;

    public FPRProcessor(FprHandle fprHandle, Map<String, AuditIssue> auditIssueMap, AuditProcessor auditProcessor) {
        this.fprHandle = fprHandle;
        this.auditIssueMap = auditIssueMap;
        this.auditProcessor = auditProcessor;
    }

    /**
     * Processes the main components of the FPR.
     *
     * @param streamingFVDLProcessor The processor for handling the audit.fvdl file.
     * @return A list of all vulnerabilities found in the FVDL.
     */
    public List<Vulnerability> process(StreamingFVDLProcessor streamingFVDLProcessor) {
        logger.info("FPR Processing started");
    try{
        this.fprInfo = new FPRInfo(this.fprHandle);

        FilterTemplateParser filterTemplateParser = new FilterTemplateParser(this.fprHandle, auditProcessor);
        Optional<FilterTemplate> filterTemplateOpt = filterTemplateParser.parseFilterTemplate();


        if (filterTemplateOpt.isPresent()) {
            FilterTemplate filterTemplate = filterTemplateOpt.get();
            this.fprInfo.setFilterTemplate(filterTemplate);

            filterTemplate.getFilterSets().stream()
                    .filter(FilterSet::isEnabled)
                    .findFirst()
                    .ifPresent(this.fprInfo::setDefaultEnabledFilterSet);

            logger.debug("Filter template loaded successfully.");
            if (this.fprInfo.getDefaultEnabledFilterSet().isPresent()) {
                logger.info("Found default enabled filter set: '{}'", this.fprInfo.getDefaultEnabledFilterSet().get().getTitle());
            } else {
                logger.info("No default enabled filter set found in template.");
            }
        } else {
            logger.warn("No filter template found. Proceeding without any available filter sets.");
        }

        logger.debug("Audit.xml Issues found: {}", auditIssueMap.size());

        try (ZipFile zipFile = new ZipFile(fprHandle.getFprPath().toFile())) {
            streamingFVDLProcessor.parse(zipFile, "audit.fvdl");
        }

        //List<Vulnerability> vulnerabilities = fvdlProcessor.processXML();

        List<Vulnerability> vulnerabilities = streamingFVDLProcessor.getVulnerabilities();
        applyAuditIssueData(vulnerabilities);
        logger.info("Parsed {} vulnerabilities from FVDL.", vulnerabilities.size());

        return vulnerabilities;
        } catch (AviatorTechnicalException e) {
            throw e;
        } catch (Exception e) {
        logger.error("Unexpected error during FPR processing", e);
        throw new AviatorTechnicalException("Unexpected error during FPR processing.", e);
        }
    }

    private void applyAuditIssueData(List<Vulnerability> vulnerabilities) {
        vulnerabilities.stream()
            .filter(vulnerability -> vulnerability != null && vulnerability.getInstanceID() != null)
            .forEach(this::applyAuditIssueData);
    }

    private void applyAuditIssueData(Vulnerability vulnerability) {
        AuditIssue auditIssue = auditIssueMap.get(vulnerability.getInstanceID());
        if (auditIssue == null) {
            return;
        }

        vulnerability.setSuppressed(auditIssue.isSuppressed());
        vulnerability.setAudited(isAudited(auditIssue));

        String issueStatus = resolveIssueStatus(auditIssue);
        if (issueStatus != null) {
            vulnerability.setIssueStatus(issueStatus);
        }

        List<AuditIssue.Comment> threadedComments = auditIssue.getThreadedComments();
        if (threadedComments != null && !threadedComments.isEmpty()) {
            vulnerability.setLastComment(threadedComments.get(threadedComments.size() - 1).getContent());
            String commentUsers = threadedComments.stream()
                .map(AuditIssue.Comment::getUsername)
                .filter(username -> username != null && !username.isBlank())
                .distinct()
                .collect(Collectors.joining(" "));
            vulnerability.setCommentUsers(commentUsers);
            vulnerability.setHistoryUsers(commentUsers);
        }
    }

    private boolean isAudited(AuditIssue auditIssue) {
        Map<String, String> tags = auditIssue.getTags();
        if (tags == null) {
            return false;
        }

        String auditorStatusValue = tags.get(Constants.AUDITOR_STATUS_TAG_ID);
        if (!isPendingReviewValue(auditorStatusValue)) {
            return true;
        }

        if (auditIssue.isSuppressed()) {
            return true;
        }

        if (tags.containsKey(Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID)) {
            return true;
        }

        String analysisTagValue = tags.get(Constants.ANALYSIS_TAG_ID);
        return analysisTagValue != null
            && !analysisTagValue.equalsIgnoreCase("Not Set")
            && !analysisTagValue.equalsIgnoreCase(Constants.PENDING_REVIEW)
            && !StringUtil.isEmpty(analysisTagValue);
    }

    private boolean isPendingReviewValue(String value) {
        return StringUtil.isEmpty(value)
            || value.equalsIgnoreCase("Pending Review")
            || value.equalsIgnoreCase(Constants.PENDING_REVIEW);
    }

    private String resolveIssueStatus(AuditIssue auditIssue) {
        Map<String, String> tags = auditIssue.getTags();
        if (tags == null || tags.isEmpty()) {
            return null;
        }

        String[] candidateTagIds = {
            Constants.AUDITOR_STATUS_TAG_ID,
            Constants.FOD_TAG_ID,
            Constants.ANALYSIS_TAG_ID,
            Constants.AVIATOR_STATUS_TAG_ID
        };

        for (String tagId : candidateTagIds) {
            String value = tags.get(tagId);
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }

        return null;
    }
}
