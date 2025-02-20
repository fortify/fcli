package com.fortify.cli.aviator.core;

import com.auth0.jwt.JWT;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
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

    public static File auditFPR(File file, String token, String url) throws AviatorSimpleException, AviatorTechnicalException, IOException {
        AviatorLoggingConfigure.configureLogger();
        logger.info("Starting FPR audit process for file: {}", file.getPath());

        String tenantId = extractTenantIdFromToken(token);

        Path extractedPath;
        try {
            extractedPath = ZipUtils.extractZip(file.getPath());
            logger.debug("Extracted FPR to path: {}", extractedPath);
        } catch (IOException e) {
            logger.error("Failed to extract FPR file: {}", file.getPath(), e);
            throw new AviatorTechnicalException("Unable to extract FPR file due to an I/O error.", e);
        }

        if (!FPRLoadingUtil.isValidFpr(file.getPath())) {
            logger.error("Invalid FPR file: {}", file);
            throw new AviatorSimpleException("Invalid FPR file format.");
        }

        if (!FPRLoadingUtil.hasSource(file)) {
            logger.error("FPR file does not contain source code: {}", file);
            throw new AviatorSimpleException("FPR file does not contain source code.");
        }

        logger.info("FPR validation successful");

        ExtensionsConfig extensionsConfig;
        try {
            extensionsConfig = ResourceUtil.loadYamlConfig("extensions_config.yaml", ExtensionsConfig.class);
            if (extensionsConfig == null) {
                logger.error("Failed to load extensions configuration");
                throw new AviatorSimpleException("Failed to load extensions configuration.");
            }
        } catch (IOException e) {
            logger.error("Failed to load extensions configuration file: {}", e.getMessage(), e);
            throw new AviatorTechnicalException("Unable to load extensions configuration due to an I/O error.", e);
        }

        FileTypeLanguageMapperUtil.initializeConfig(extensionsConfig);

        try {
            AuditProcessor auditProcessor = new AuditProcessor(extractedPath, file.getPath());
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();

            FPRProcessor fprProcessor = new FPRProcessor(file.getPath(), extractedPath, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process();
            FPRInfo fprInfo = fprProcessor.getFprInfo();

            Map<String, AuditResponse> auditResponses = new ConcurrentHashMap<>();
            IssueAuditor issueAuditor = new IssueAuditor(vulnerabilities, auditProcessor, auditIssueMap, fprInfo, false);

            issueAuditor.performAudit(auditResponses, tenantId, token, fprInfo.getBuildId(), url);
            logger.info("Completed audit process, received {} responses", auditResponses.size());

            File updatedFile = auditProcessor.updateAndSaveAuditXml(auditResponses, fprInfo.getResultsTag());
            logger.info("FPR audit process completed successfully");
            return updatedFile;
        } catch (Exception e) {
            logger.error("I/O error during FPR audit processing: {}", file.getPath(), e);
            throw new AviatorTechnicalException("Failed to process FPR file due to an I/O error.", e);
        }
    }

    private static String extractTenantIdFromToken(String token) throws AviatorSimpleException {
        try {
            DecodedJWT jwt = JWT.decode(token);
            String tenantId = jwt.getClaim("tenantId").asString();
            if (tenantId == null || tenantId.isEmpty()) {
                logger.error("JWT token does not contain a 'tenantId' claim");
                throw new AviatorSimpleException("JWT token does not contain a 'tenantId' claim.");
            }
            return tenantId;
        } catch (JWTDecodeException e) {
            logger.error("Failed to parse JWT token: {}", e.getMessage());
            throw new AviatorSimpleException("Unable to extract tenantId from token due to invalid JWT format.", e);
        }
    }
}