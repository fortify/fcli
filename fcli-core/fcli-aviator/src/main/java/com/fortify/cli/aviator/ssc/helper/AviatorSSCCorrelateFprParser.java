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
package com.fortify.cli.aviator.ssc.helper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.fpr.FPRProcessor;
import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Encapsulates FPR parsing for the correlate-sast-dast command.
 * Handles both SAST (FVDL-based) and DAST (WebInspect-based) FPRs,
 * including parser comparison logging for the streaming WebInspect parser.
 */
public final class AviatorSSCCorrelateFprParser {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCCorrelateFprParser.class);

    private AviatorSSCCorrelateFprParser() {}

    /**
     * Holds the result of parsing either a SAST or DAST FPR.
     */
    public static class ParseResult {
        public List<Vulnerability> vulnerabilities = new ArrayList<>();
        public List<DastIssue> dastIssues = new ArrayList<>();
        public Map<String, AuditIssue> auditIssueMap;
        public String buildId;
        public String scanGuid;
    }

    /**
     * Parses a SAST FPR and returns vulnerabilities, audit map, scanGuid, and buildId.
     */
    public static ParseResult parseSastFpr(Path sastfpr) {
        try (FprHandle fprHandle = new FprHandle(sastfpr)) {
            fprHandle.validate();
            if (!Files.exists(fprHandle.getPath("/audit.xml"))) {
                throw new FcliSimpleException("SAST FPR does not contain audit.xml");
            }
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
            StreamingFVDLProcessor fvdlProcessor = new StreamingFVDLProcessor(fprHandle);
            FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
            List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);

            ParseResult result = new ParseResult();
            result.vulnerabilities = vulnerabilities;
            result.auditIssueMap = auditIssueMap;
            if (!vulnerabilities.isEmpty()) {
                result.scanGuid = vulnerabilities.get(0).getUuid();
                result.buildId = vulnerabilities.get(0).getBuildId();
            }
            return result;
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to parse SAST FPR: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a DAST FPR using the streaming WebInspect parser.
     * Also runs the DOM parser in parallel for comparison logging.
     */
    public static ParseResult parseDastFpr(Path dastfpr) {
        try (FprHandle fprHandle = new FprHandle(dastfpr)) {
            if (!Files.exists(fprHandle.getPath("/webinspect.xml"))) {
                throw new FcliSimpleException("DAST FPR does not contain webinspect.xml");
            }
            if (!Files.exists(fprHandle.getPath("/audit.xml"))) {
                throw new FcliSimpleException("DAST FPR does not contain audit.xml");
            }
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();

            StreamingWebInspectParser streamingParser = new StreamingWebInspectParser(fprHandle);
            List<DastIssue> streamingDastIssues = streamingParser.parse();

            for (DastIssue issue : streamingDastIssues) {
                if (issue.getId() != null && auditIssueMap.containsKey(issue.getId())) {
                    issue.setSuppressed(auditIssueMap.get(issue.getId()).isSuppressed());
                }
            }

            ParseResult result = new ParseResult();
            result.dastIssues = streamingDastIssues;
            result.auditIssueMap = auditIssueMap;
            return result;
        } catch (FcliSimpleException e) {
            throw e;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to parse DAST FPR: " + e.getMessage(), e);
        }
    }
}
