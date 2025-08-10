package com.fortify.cli.aviator.audit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fortify.cli.aviator.audit.model.ParsedFprData;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.filter.FilterSet;
import com.fortify.cli.aviator.fpr.filter.FilterSetSelector;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
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
import com.fortify.cli.aviator.util.FPRLoadingUtil;
import com.fortify.cli.aviator.util.ResourceUtil;
import com.fortify.cli.aviator.util.ZipUtils;

public class AuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(AuditFPR.class);

    public static FPRAuditResult auditFPR(File fprFile, String token, String url, String appVersion,
                                          String sscAppName, String sscAppVersion,
                                          IAviatorLogger logger, String tagMappingPath,
                                          String filterSetNameOrId, boolean ignoreFilters,
                                          List<String> priorities)
            throws AviatorSimpleException, AviatorTechnicalException {

        LOG.info("Starting FPR audit process for file: {}", fprFile.getPath());
        AviatorConfigManager.getInstance();

        ParsedFprData parsedData = prepareAndParseFpr(fprFile);
        TagMappingConfig tagMappingConfig = loadTagMappingConfig(tagMappingPath);

        FilterSet activeFilterSet = FilterSetSelector.select(
                parsedData.fprInfo, priorities, filterSetNameOrId, ignoreFilters);

        Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
        AuditOutcome auditOutcome = performAviatorAudit(
                parsedData, logger, token, appVersion, url, sscAppName, sscAppVersion,
                auditResponses, activeFilterSet
        );

        return finalizeFprAudit(
                auditOutcome, auditResponses, parsedData.auditProcessor,
                tagMappingConfig, parsedData.fprInfo, parsedData.fvdlProcessor
        );
    }

    private static ParsedFprData prepareAndParseFpr(File fprFile) {
        try {
            FPRLoadingUtil.validateFpr(fprFile);
            Path extractedPath = ZipUtils.extractZip(fprFile.getPath());

            AuditProcessor auditProcessor = new AuditProcessor(extractedPath, fprFile.getPath());
            FVDLProcessor fvdlProcessor = new FVDLProcessor(extractedPath);

            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            FPRProcessor fprProcessor = new FPRProcessor(fprFile.getPath(), extractedPath, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);
            FPRInfo fprInfo = fprProcessor.getFprInfo();

            return new ParsedFprData(auditIssueMap, vulnerabilities, fprInfo, auditProcessor, fvdlProcessor);
        } catch (IOException e) {
            LOG.error("Failed to extract or read FPR: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Failed to extract or read FPR: " + e.getMessage(), e);
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
            Map<String, AuditResponse> auditResponsesToFill, FilterSet activeFilterSet) {

        IssueAuditor issueAuditor = new IssueAuditor(
                parsedData.vulnerabilities, parsedData.auditProcessor, parsedData.auditIssueMap,
                parsedData.fprInfo, sscAppName, sscAppVersion, activeFilterSet, logger
        );
        AuditOutcome outcome = issueAuditor.performAudit(
                auditResponsesToFill, token, appVersion, parsedData.fprInfo.getBuildId(), url
        );
        LOG.info("Completed Aviator audit, received {} responses", auditResponsesToFill.size());
        return outcome;
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