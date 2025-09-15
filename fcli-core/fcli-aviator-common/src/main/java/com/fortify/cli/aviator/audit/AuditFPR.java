package com.fortify.cli.aviator.audit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fortify.cli.aviator.audit.model.AuditFprOptions;
import com.fortify.cli.aviator.audit.model.FilterSelection;
import com.fortify.cli.aviator.audit.model.ParsedFprData;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.FilterSetSelector;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.util.FprHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.audit.model.AuditOutcome;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.config.TagMappingConfig;
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

        // --- STAGE 2: FILTER SELECTION (DELEGATED) ---
        FilterSelection filterSelection = FilterSetSelector.select(
                parsedData.fprInfo, options.getPriorities(), options.getFilterSetNameOrId(), options.isNoFilterSet(),
                options.getFolderNames()
        );

        // --- STAGE 3: AUDITING ---
        Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
        AuditOutcome auditOutcome = performAviatorAudit(
                parsedData, options.getLogger(), options.getToken(), options.getAppVersion(), options.getUrl(), options.getSscAppName(), options.getSscAppVersion(),
                auditResponses, filterSelection
        );

        // --- STAGE 4: FINALIZATION ---
        return finalizeFprAudit(
                auditOutcome, auditResponses, parsedData.auditProcessor,
                tagMappingConfig, parsedData.fprInfo, parsedData.fvdlProcessor
        );
    }

    private static ParsedFprData prepareAndParseFpr(FprHandle fprHandle) {
        try {
            // Processors now take the FprHandle directly, no more extracted path
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            FVDLProcessor fvdlProcessor = new FVDLProcessor(fprHandle);

            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);
            FPRInfo fprInfo = fprProcessor.getFprInfo();

            return new ParsedFprData(auditIssueMap, vulnerabilities, fprInfo, auditProcessor, fvdlProcessor);
        } catch (Exception e) {
            LOG.error("A critical error occurred during FPR processing.", e);
            throw new AviatorTechnicalException("Failed to process FPR contents.", e);
        }
    }

    private static TagMappingConfig loadTagMappingConfig(String tagMappingFilePath) {
        if (tagMappingFilePath != null && !tagMappingFilePath.trim().isEmpty()) {
            LOG.info("Loading user-provided tag mapping from: {}", tagMappingFilePath);
            return ResourceUtil.loadYamlFile(new File(tagMappingFilePath), TagMappingConfig.class);
        } else {
            LOG.info("Using default tag mapping configuration.");
            return AviatorConfigManager.getInstance().getDefaultTagMappingConfig();
        }
    }

    private static AuditOutcome performAviatorAudit(
            ParsedFprData parsedData, IAviatorLogger logger,
            String token, String appVersion, String url, String sscAppName, String sscAppVersion,
            Map<String, AuditResponse> auditResponsesToFill, FilterSelection filterSelection) {

        IssueAuditor issueAuditor = new IssueAuditor(
                parsedData.vulnerabilities, parsedData.auditProcessor, parsedData.auditIssueMap,
                parsedData.fprInfo, sscAppName, sscAppVersion, filterSelection, logger
        );
        return issueAuditor.performAudit(
                auditResponsesToFill, token, appVersion, parsedData.fprInfo.getBuildId(), url
        );
    }

    private static FPRAuditResult finalizeFprAudit(
            AuditOutcome auditOutcome, Map<String, AuditResponse> auditResponses,
            AuditProcessor auditProcessor, TagMappingConfig tagMappingConfig,
            FPRInfo fprInfo, FVDLProcessor fvdlProcessor) {

        int totalIssuesToAudit = auditOutcome.getTotalIssuesToAudit();
        if (auditResponses.isEmpty() && totalIssuesToAudit == 0) {
            LOG.info("No issues were audited, skipping update and upload");
            return new FPRAuditResult(null, "SKIPPED", "No issues to audit", 0, totalIssuesToAudit);
        }

        long issuesSuccessfullyAudited = auditResponses.values().stream()
                .filter(response -> "SUCCESS".equals(response.getStatus())).count();

        String status = (issuesSuccessfullyAudited == totalIssuesToAudit) ? "AUDITED" :
                (issuesSuccessfullyAudited > 0) ? "PARTIALLY_AUDITED" : "FAILED";

        File updatedFile = auditProcessor.updateAndSaveAuditAndRemediationsXml(auditResponses, tagMappingConfig, fprInfo, fvdlProcessor);
        LOG.info("FPR audit process completed with status: {}", status);
        return new FPRAuditResult(updatedFile, status, null, (int) issuesSuccessfullyAudited, totalIssuesToAudit);
    }
}