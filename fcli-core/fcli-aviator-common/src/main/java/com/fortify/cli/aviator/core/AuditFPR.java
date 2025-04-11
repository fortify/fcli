package com.fortify.cli.aviator.core;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.core.model.AuditOutcome;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.core.model.FPRAuditResult;
import com.fortify.cli.aviator.fpr.*;
import com.fortify.cli.aviator.util.ExtensionsConfig;
import com.fortify.cli.aviator.util.FPRLoadingUtil;
import com.fortify.cli.aviator.util.FileTypeLanguageMapperUtil;
import com.fortify.cli.aviator.util.TagMappingConfig;
import com.fortify.cli.aviator.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuditFPR {
    private static final Logger LOG = LoggerFactory.getLogger(AuditFPR.class);

    public static FPRAuditResult auditFPR(File file, String token, String url, String appVersion, IAviatorLogger logger, String tagMappingFilePath)
            throws AviatorSimpleException, AviatorTechnicalException {
        LOG.info("Starting FPR audit process for file: {}", file.getPath());

        Path extractedPath;
        try {
            extractedPath = ZipUtils.extractZip(file.getPath());
            LOG.debug("Extracted FPR to path: {}", extractedPath);
        } catch (IOException e) {
            LOG.error("Failed to extract FPR file: {}", file.getPath(), e);
            throw new AviatorTechnicalException("Unable to extract FPR file due to an I/O error.", e);
        }

        try {
            if (!FPRLoadingUtil.isValidFpr(file.getPath())) {
                LOG.error("Invalid FPR file: {}", file);
                throw new AviatorSimpleException("Invalid FPR file format.");
            }
            if (!FPRLoadingUtil.hasSource(file)) {
                LOG.error("FPR file does not contain source code: {}", file);
                throw new AviatorSimpleException("FPR file does not contain source code.");
            }
        } catch (IOException e) {
            LOG.error("I/O error checking FPR source presence: {}", file.getPath(), e);
            throw new AviatorTechnicalException("I/O error checking FPR source presence.", e);
        }
        LOG.info("FPR validation successful");

        ExtensionsConfig extensionsConfig;
        try {
            Yaml yaml = new Yaml();
            try (InputStream inputStream = AuditFPR.class.getClassLoader().getResourceAsStream("extensions_config.yaml")) {
                if (inputStream == null) {
                    throw new AviatorSimpleException("Resource not found: extensions_config.yaml");
                }
                extensionsConfig = yaml.loadAs(inputStream, ExtensionsConfig.class);
                if (extensionsConfig == null) {
                    LOG.error("Failed to load extensions configuration (result was null)");
                    throw new AviatorTechnicalException("Failed to load required extensions configuration.");
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load extensions configuration file: {}", e.getMessage(), e);
            throw new AviatorTechnicalException("Unable to load extensions configuration due to an I/O error.", e);
        }

        TagMappingConfig tagMappingConfig;
        Yaml yaml = new Yaml();
        try {
            if (tagMappingFilePath != null && !tagMappingFilePath.trim().isEmpty()) {
                try (FileInputStream fis = new FileInputStream(tagMappingFilePath)) {
                    tagMappingConfig = yaml.loadAs(fis, TagMappingConfig.class);
                    if (tagMappingConfig == null) {
                        throw new AviatorSimpleException("Tag mapping file '" + tagMappingFilePath + "' is invalid or empty.");
                    }
                }
            } else {
                try (InputStream inputStream = AuditFPR.class.getClassLoader().getResourceAsStream("default_tag_mapping.yaml")) {
                    if (inputStream == null) {
                        throw new AviatorSimpleException("Resource not found: default_tag_mapping.yaml");
                    }
                    tagMappingConfig = yaml.loadAs(inputStream, TagMappingConfig.class);
                    if (tagMappingConfig == null) {
                        throw new AviatorTechnicalException("Default tag mapping configuration could not be loaded.");
                    }
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to load tag mapping configuration: {}", e.getMessage(), e);
            throw new AviatorTechnicalException("Failed to load tag mapping configuration due to an I/O error.", e);
        } catch (Exception e) {
            LOG.error("Invalid tag mapping file format in '{}': {}", tagMappingFilePath, e.getMessage(), e);
            throw new AviatorSimpleException("Invalid tag mapping file format in '" + tagMappingFilePath + "': " + e.getMessage(), e);
        }

        FileTypeLanguageMapperUtil.initializeConfig(extensionsConfig);

        AuditProcessor auditProcessor = new AuditProcessor(extractedPath, file.getPath());
        Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();

        FPRProcessor fprProcessor = new FPRProcessor(file.getPath(), extractedPath, auditIssueMap, auditProcessor);
        List<Vulnerability> vulnerabilities = fprProcessor.process();
        FPRInfo fprInfo = fprProcessor.getFprInfo();

        Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
        IssueAuditor issueAuditor = new IssueAuditor(vulnerabilities, auditProcessor, auditIssueMap, fprInfo, false, logger);
        AuditOutcome outcome = issueAuditor.performAudit(auditResponses, token, appVersion, fprInfo.getBuildId(), url);

        LOG.info("Completed audit process, received {} responses", auditResponses.size());

        int totalIssuesToAudit = outcome.getTotalIssuesToAudit();
        if (auditResponses.isEmpty()) {
            if (totalIssuesToAudit == 0) {
                LOG.info("No issues were audited, skipping update and upload");
                return new FPRAuditResult(null, "SKIPPED", "No issues to audit", 0, totalIssuesToAudit);
            } else {
                LOG.error("No audit responses received for {} issues", totalIssuesToAudit);
                return new FPRAuditResult(null, "FAILED", "No audit responses received", 0, totalIssuesToAudit);
            }
        }

        int issuesSuccessfullyAudited = (int) auditResponses.values().stream()
                .filter(response -> "SUCCESS".equals(response.getStatus()))
                .count();

        String status;
        if (issuesSuccessfullyAudited == totalIssuesToAudit) {
            status = "AUDITED";
        } else if (issuesSuccessfullyAudited > 0) {
            status = "PARTIALLY_AUDITED";
        } else {
            status = "FAILED";
        }

        File updatedFile = auditProcessor.updateAndSaveAuditXml(auditResponses, tagMappingConfig);
        LOG.info("FPR audit process completed with status: {}", status);
        return new FPRAuditResult(updatedFile, status, null, issuesSuccessfullyAudited, totalIssuesToAudit);
    }

    public static FPRAuditResult auditFPR(File file, String token, String url, String appVersion, IAviatorLogger logger)
            throws AviatorSimpleException, AviatorTechnicalException {
        return auditFPR(file, token, url, appVersion, logger, null);
    }
}