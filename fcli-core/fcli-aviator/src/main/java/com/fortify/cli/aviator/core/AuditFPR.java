package com.fortify.cli.aviator.core;

import com.fortify.cli.aviator.config.AviatorLoggingConfigure;
import com.fortify.cli.aviator.core.model.AuditResponse;
import com.fortify.cli.aviator.fpr.*;
import com.fortify.cli.aviator.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuditFPR {
    private static final Logger logger = LoggerFactory.getLogger(AuditFPR.class);

    public static File auditFpr(File file, String token, String tenantName, String url) throws IOException {

        AviatorLoggingConfigure.configureLogger();

        logger.info("Starting FPR audit process for file: {}", file.getPath());

        Path extractedPath = ZipUtils.extractZip(file.getPath());
        logger.debug("Extracted FPR to path: {}", extractedPath);

        try {
            if (!FPRLoadingUtil.isValidFpr(file.getPath())) {
                logger.error("Invalid FPR file: {}", file);
                throw new IOException("Invalid FPR file format.");
            }

            if (!FPRLoadingUtil.hasSource(new File(file.getPath()))) {
                logger.error("FPR file must contain source code: {}", file);
                throw new IOException("FPR file does not contain source code.");
            }

            logger.info("FPR validation successful");
        } catch (IOException e) {
            logger.error("Error validating FPR file: {}", file, e);
            System.exit(-1);
        }

        IssueAuditor issueAuditor;
        try {
            logger.info("Opening file: {}", file.getPath());

            ExtensionsConfig extensionsConfig = ResourceUtil.loadYamlConfig("extensions_config.yaml", ExtensionsConfig.class);
            if (extensionsConfig == null) {
                logger.error("Failed to load extensions configuration");
                System.exit(-1);
            }

            FileTypeLanguageMapperUtil.initializeConfig(extensionsConfig);

            AuditProcessor auditProcessor = new AuditProcessor(extractedPath, file.getPath());
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();

            FPRProcessor fprProcessor = new FPRProcessor(file.getPath(), extractedPath, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process();

            FPRInfo fprInfo = fprProcessor.getFprInfo();

            Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();

            issueAuditor = new IssueAuditor(vulnerabilities, auditProcessor, auditIssueMap, fprInfo, false);

            issueAuditor.performAudit(auditResponses, token, tenantName, fprInfo.getBuildId(),url);
            logger.info("Completed audit process, received {} responses", auditResponses.size());

            File updatedFile = auditProcessor.updateAndSaveAuditXml(auditResponses, fprInfo.getResultsTag());

            logger.info("FPR audit process completed successfully");
            return updatedFile;

        } catch (Exception e) {
            logger.error("Unexpected error during FPR audit process", e);
            throw new RuntimeException(e);
        }
    }
}