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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.AuditOutcome;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.audit.model.FilterSelection;
import com.fortify.cli.aviator.audit.model.ParsedFprData;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.FilterSetSelector;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.aviator.util.ResourceUtil;

public class AuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(AuditFPR.class);

    public static FPRAuditResult auditFPR(AuditFprOptions options)
            throws AviatorSimpleException, AviatorTechnicalException {

        LOG.info("Starting FPR audit process for file: {}", options.getFprHandle().getFprPath());
        options.getFprHandle().validate();
        AviatorConfigManager.getInstance();

        // --- STAGE 1: PARSING ---
        ParsedFprData parsedData = prepareAndParseFpr(options.getFprHandle());
        TagMappingConfig tagMappingConfig = loadTagMappingConfig(options.getTagMappingPath());
        Map<String, String> issueCategoryLookup = tagMappingConfig.requiresCategoryForSuppressionEvaluation()
            ? buildIssueCategoryLookup(parsedData.vulnerabilities)
            : Map.of();

        // --- STAGE 2: FILTER SELECTION (DELEGATED) ---
        FilterSelection filterSelection = FilterSetSelector.select(
                parsedData.fprInfo, options.getFilterSetNameOrId(), options.isNoFilterSet(),
                options.getFolderNames()
        );

        // --- STAGE 3: AUDITING ---
        Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
        AuditOutcome auditOutcome = performAviatorAudit(
                parsedData, options.getLogger(), options.getToken(), options.getAppVersion(), options.getUrl(), options.getSscAppName(), options.getSscAppVersion(),
                auditResponses, filterSelection, options.getFprHandle(), options.getFolderPriorityOrder()
        );

        // --- STAGE 4: FINALIZATION ---
        return finalizeFprAudit(
                auditOutcome, auditResponses, parsedData.auditProcessor,
            tagMappingConfig, issueCategoryLookup, parsedData.fprInfo
        );
    }

    private static ParsedFprData prepareAndParseFpr(FprHandle fprHandle) {
        try {
            // Processors now take the FprHandle directly, no more extracted path
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            //FVDLProcessor fvdlProcessor = new FVDLProcessor(fprHandle);
            StreamingFVDLProcessor streamingFVDLProcessor = new StreamingFVDLProcessor(fprHandle);

            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process(streamingFVDLProcessor);
            FPRInfo fprInfo = fprProcessor.getFprInfo();

            return new ParsedFprData(auditIssueMap, vulnerabilities, fprInfo, auditProcessor, streamingFVDLProcessor);
        } catch (Exception e) {
            LOG.error("A critical error occurred during FPR processing.", e);
            throw new AviatorTechnicalException("Failed to process FPR contents.", e);
        }
    }

    private static TagMappingConfig loadTagMappingConfig(String tagMappingFilePath) {
        TagMappingConfig tagMappingConfig;
        if (tagMappingFilePath != null && !tagMappingFilePath.trim().isEmpty()) {
            LOG.info("Loading user-provided tag mapping from: {}", tagMappingFilePath);
            tagMappingConfig = ResourceUtil.loadYamlFile(new File(tagMappingFilePath), TagMappingConfig.class);
        } else {
            LOG.info("Using default tag mapping configuration.");
            tagMappingConfig = AviatorConfigManager.getInstance().getDefaultTagMappingConfig();
        }

        tagMappingConfig.validate();
        return tagMappingConfig;
    }

    private static Map<String, String> buildIssueCategoryLookup(List<Vulnerability> vulnerabilities) {
        Map<String, String> issueCategoryLookup = new HashMap<>();
        for (Vulnerability vulnerability : vulnerabilities) {
            String instanceId = vulnerability.getInstanceID();
            if (instanceId != null && !instanceId.isBlank()) {
                issueCategoryLookup.putIfAbsent(instanceId, vulnerability.getCategory());
            }
        }
        return issueCategoryLookup;
    }

    private static AuditOutcome performAviatorAudit(
            ParsedFprData parsedData, IAviatorLogger logger,
            String token, String appVersion, String url, String sscAppName, String sscAppVersion,
            Map<String, AuditResponse> auditResponsesToFill, FilterSelection filterSelection, FprHandle fprHandle, List<String> folderPriorityOrder) {

        IssueAuditor issueAuditor = new IssueAuditor(
                parsedData.vulnerabilities,
                parsedData.auditProcessor,
                parsedData.auditIssueMap,
                parsedData.fprInfo,
                sscAppName,
                sscAppVersion,
                filterSelection,
                logger,
                folderPriorityOrder
        );
        return issueAuditor.performAudit(
                auditResponsesToFill, token, appVersion, parsedData.fprInfo.getBuildId(), url, fprHandle
        );
    }

    private static FPRAuditResult finalizeFprAudit(
            AuditOutcome auditOutcome, Map<String, AuditResponse> auditResponses,
            AuditProcessor auditProcessor, TagMappingConfig tagMappingConfig,
            Map<String, String> issueCategoryLookup, FPRInfo fprInfo) {

        int totalIssuesToAudit = auditOutcome.getTotalIssuesToAudit();
        if (auditResponses.isEmpty()) {
            if (totalIssuesToAudit == 0) {
                LOG.info("No issues were audited, skipping update and upload");
                return new FPRAuditResult(null, "SKIPPED", "No issues to audit", 0, totalIssuesToAudit);
            } else {
                LOG.error("No audit responses received for {} issues", totalIssuesToAudit);
                return new FPRAuditResult(null, "FAILED", "No audit responses received from server", 0, totalIssuesToAudit);
            }
        }

        long issuesSuccessfullyAudited = auditResponses.values().stream()
                .filter(response -> "SUCCESS".equalsIgnoreCase(response.getStatus()))
                .count();

        String status;
        String message = null;

        if (issuesSuccessfullyAudited == totalIssuesToAudit) {
            status = "AUDITED";
        } else if (issuesSuccessfullyAudited > 0) {
            status = "PARTIALLY_AUDITED";
        } else {
            status = "FAILED";
            String commonFailureReason = auditResponses.values().stream()
                    .map(AuditResponse::getStatusMessage)
                    .filter(msg -> msg != null && !msg.isBlank())
                    .findFirst()
                    .orElse("see logs for details");

            if (commonFailureReason.startsWith("Client-side pre-processing error: ")) {
                commonFailureReason = commonFailureReason.substring("Client-side pre-processing error: ".length());
            }
            message = String.format("All %d issues failed (%s)", totalIssuesToAudit, commonFailureReason);
        }

        File updatedFile = null;
        if (issuesSuccessfullyAudited > 0) {
            updatedFile = auditProcessor.updateAndSaveAuditAndRemediationsXml(
                    auditResponses, tagMappingConfig, issueCategoryLookup, fprInfo);
        }

        LOG.info("FPR audit process completed with status: {}", status);
        return new FPRAuditResult(updatedFile, status, message, (int) issuesSuccessfullyAudited, totalIssuesToAudit);
    }
}
