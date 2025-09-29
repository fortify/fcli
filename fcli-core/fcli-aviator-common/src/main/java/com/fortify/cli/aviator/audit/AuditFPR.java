package com.fortify.cli.aviator.audit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.config.AviatorConfigManager;
import com.fortify.cli.aviator._common.exception.AviatorBugException;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.audit.model.AuditOutcome;
import com.fortify.cli.aviator.audit.model.AuditResponse;
import com.fortify.cli.aviator.audit.model.FPRAuditResult;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.fpr.model.Vulnerability;
import com.fortify.cli.aviator.config.TagMappingConfig;
import com.fortify.cli.aviator.util.FPRLoadingUtil;
import com.fortify.cli.aviator.util.ResourceUtil;
import com.fortify.cli.aviator.util.ZipUtils;

public class AuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(AuditFPR.class);

    private static class FprSetup {
        final Path extractedPath;
        final FVDLProcessor fvdlProcessor;
        final TagMappingConfig tagMappingConfig;
        final AuditProcessor auditProcessor;

        FprSetup(Path extractedPath, TagMappingConfig tagMappingConfig, AuditProcessor auditProcessor, FVDLProcessor fvdlProcessor) {
            this.extractedPath = extractedPath;
            this.tagMappingConfig = tagMappingConfig;
            this.auditProcessor = auditProcessor;
            this.fvdlProcessor = fvdlProcessor;
        }
    }

    private static class ParsedFprData {
        final Map<String, AuditIssue> auditIssueMap;
        final List<Vulnerability> vulnerabilities;
        final FPRInfo fprInfo;

        ParsedFprData(Map<String, AuditIssue> auditIssueMap, List<Vulnerability> vulnerabilities, FPRInfo fprInfo) {
            this.auditIssueMap = auditIssueMap;
            this.vulnerabilities = vulnerabilities;
            this.fprInfo = fprInfo;
        }
    }


    public static FPRAuditResult auditFPR(File fprFile, String token, String url, String appVersion, String SSCApplicationName,String SSCApplicationVersion , IAviatorLogger logger, String tagMappingFilePath)
            throws AviatorSimpleException, AviatorTechnicalException {
        LOG.info("Starting FPR audit process for file: {}", fprFile.getPath());

        AviatorConfigManager.getInstance();

        Path extractedTempPath;
        try {
            extractedTempPath = ZipUtils.extractZip(fprFile.getPath());
        } catch (IOException e) {
            LOG.error("Failed to extract FPR: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("Failed to extract FPR: " + e.getMessage(), e);
        }

        AuditProcessor auditProcessor = new AuditProcessor(extractedTempPath, fprFile.getPath());
        FVDLProcessor fvdlProcessor = new FVDLProcessor(extractedTempPath);

        FprSetup setup = prepareFprAndLoadConfigs(fprFile, tagMappingFilePath, auditProcessor, fvdlProcessor, extractedTempPath);
        ParsedFprData parsedData = parseFpr(extractedTempPath, fprFile.getPath(), setup.auditProcessor, setup.fvdlProcessor);


        Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
        AuditOutcome auditOutcome = performAviatorAudit(
                parsedData, setup.auditProcessor, logger, token, appVersion, url, SSCApplicationName, SSCApplicationVersion, auditResponses
        );

        return finalizeFprAudit(auditOutcome, auditResponses, setup.auditProcessor, setup.tagMappingConfig, parsedData.fprInfo, setup.fvdlProcessor);
    }

    private static FprSetup prepareFprAndLoadConfigs(File fprFile, String tagMappingFilePath, AuditProcessor auditProcessor, FVDLProcessor fvdlProcessor, Path extractedPath)
            throws AviatorTechnicalException, AviatorSimpleException, AviatorBugException {

        try {
            if (!FPRLoadingUtil.isValidFpr(fprFile.getPath())) {
                LOG.error("Invalid FPR file: {}", fprFile);
                throw new AviatorSimpleException("Invalid FPR file format.");
            }
            if (!FPRLoadingUtil.hasSource(fprFile)) {
                LOG.error("FPR file does not contain source code: {}", fprFile);
                throw new AviatorSimpleException("FPR file does not contain source code.");
            }
        } catch (IOException e) {
            LOG.error("I/O error checking FPR source presence: {}", fprFile.getPath(), e);
            throw new AviatorTechnicalException("I/O error checking FPR source presence.", e);
        }
        LOG.info("FPR validation successful");


        TagMappingConfig tagMappingConfig;
        if (tagMappingFilePath != null && !tagMappingFilePath.trim().isEmpty()) {
            LOG.info("Loading user-provided tag mapping from: {}", tagMappingFilePath);
            tagMappingConfig = ResourceUtil.loadYamlFile(new File(tagMappingFilePath), TagMappingConfig.class);
        } else {
            LOG.info("Using default tag mapping configuration.");
            tagMappingConfig = AviatorConfigManager.getInstance().getDefaultTagMappingConfig();
        }

        return new FprSetup(extractedPath, tagMappingConfig, auditProcessor, fvdlProcessor);
    }

    private static ParsedFprData parseFpr(Path extractedPath, String originalFprPath, AuditProcessor auditProcessor, FVDLProcessor fvdlProcessor)
            throws AviatorTechnicalException {
        Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
        FPRProcessor fprProcessor = new FPRProcessor(originalFprPath, extractedPath, auditIssueMap, auditProcessor, fvdlProcessor);
        List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);
        FPRInfo fprInfo = fprProcessor.getFprInfo();
        return new ParsedFprData(auditIssueMap, vulnerabilities, fprInfo);
    }

    private static AuditOutcome performAviatorAudit(
            ParsedFprData parsedData, AuditProcessor auditProcessor, IAviatorLogger logger,
            String token, String appVersion, String url, String SSCApplicationName, String SSCApplicationVersion ,
            Map<String, AuditResponse> auditResponsesToFill)
            throws AviatorSimpleException, AviatorTechnicalException {

        IssueAuditor issueAuditor = new IssueAuditor(
                parsedData.vulnerabilities,
                auditProcessor,
                parsedData.auditIssueMap,
                parsedData.fprInfo,
                SSCApplicationName,
                SSCApplicationVersion,
                logger
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
            FPRInfo fprInfo, FVDLProcessor fvdlProcessor)
            throws AviatorTechnicalException {

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
            updatedFile = auditProcessor.updateAndSaveAuditAndRemediationsXml(auditResponses, tagMappingConfig, fprInfo, fvdlProcessor);
        }

        LOG.info("FPR audit process completed with status: {}", status);
        return new FPRAuditResult(updatedFile, status, message, (int) issuesSuccessfullyAudited, totalIssuesToAudit);
    }
}