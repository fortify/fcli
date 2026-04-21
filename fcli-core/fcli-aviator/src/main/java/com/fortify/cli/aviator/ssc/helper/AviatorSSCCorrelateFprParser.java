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
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator.dast.DastIssue;
import com.fortify.cli.aviator.dast.StreamingWebInspectParser;
import com.fortify.cli.aviator.dast.WebInspectParser;
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
        LOG.debug("Parsing SAST FPR");
        try (FprHandle fprHandle = new FprHandle(sastfpr)) {
            fprHandle.validate();
            LOG.debug("Validation FPR handle done");
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
            AuditProcessor auditProcessor = new AuditProcessor(fprHandle);
            Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();

            WebInspectParser parser = new WebInspectParser(fprHandle);
            List<DastIssue> domIssues = parser.parse();

            StreamingWebInspectParser streamingParser = new StreamingWebInspectParser(fprHandle);
            List<DastIssue> streamingDastIssues = streamingParser.parse();

            compareParserResults(domIssues, streamingDastIssues);

            for (DastIssue issue : streamingDastIssues) {
                if (issue.getId() != null && auditIssueMap.containsKey(issue.getId())) {
                    issue.setSuppressed(auditIssueMap.get(issue.getId()).isSuppressed());
                }
            }

            ParseResult result = new ParseResult();
            result.dastIssues = streamingDastIssues;
            result.auditIssueMap = auditIssueMap;
            return result;
        } catch (Exception e) {
            throw new FcliSimpleException("Failed to parse DAST FPR: " + e.getMessage(), e);
        }
    }

    // ─── Parser comparison (development/validation utility) ───────────

    private static void compareParserResults(List<DastIssue> domIssues, List<DastIssue> staxIssues) {
        LOG.info("=== Parser Comparison: DOM vs Streaming WebInspect Parser ===");

        if (domIssues.size() != staxIssues.size()) {
            LOG.warn("Issue count MISMATCH: DOM={} vs Streaming={}", domIssues.size(), staxIssues.size());
        } else {
            LOG.info("Issue count matches: {}", domIssues.size());
        }

        int limit = Math.min(domIssues.size(), staxIssues.size());
        int mismatchCount = 0;

        for (int i = 0; i < limit; i++) {
            DastIssue dom = domIssues.get(i);
            DastIssue stax = staxIssues.get(i);
            List<String> diffs = compareIssueFields(dom, stax);

            if (!diffs.isEmpty()) {
                mismatchCount++;
                LOG.warn("Issue[{}] id='{}' has {} difference(s):", i, dom.getId(), diffs.size());
                diffs.forEach(d -> LOG.warn("  - {}", d));
            }
        }

        if (domIssues.size() > limit) {
            LOG.warn("{} extra issue(s) only in DOM parser (indices {} to {})",
                    domIssues.size() - limit, limit, domIssues.size() - 1);
        }
        if (staxIssues.size() > limit) {
            LOG.warn("{} extra issue(s) only in Streaming parser (indices {} to {})",
                    staxIssues.size() - limit, limit, staxIssues.size() - 1);
        }

        if (mismatchCount == 0 && domIssues.size() == staxIssues.size()) {
            LOG.info("Parser comparison PASSED: all {} issues are identical", domIssues.size());
        } else {
            LOG.warn("Parser comparison FAILED: {}/{} common issues have differences", mismatchCount, limit);
        }

        LOG.info("=== End Parser Comparison ===");
    }

    private static List<String> compareIssueFields(DastIssue dom, DastIssue stax) {
        List<String> diffs = new ArrayList<>();
        compareField(diffs, "id", dom.getId(), stax.getId());
        compareField(diffs, "checkTypeId", dom.getCheckTypeId(), stax.getCheckTypeId());
        compareField(diffs, "engineType", dom.getEngineType(), stax.getEngineType());
        compareField(diffs, "vulnerabilityId", dom.getVulnerabilityId(), stax.getVulnerabilityId());
        if (dom.getSeverity() != stax.getSeverity()) {
            diffs.add(String.format("severity: DOM=%d vs Streaming=%d", dom.getSeverity(), stax.getSeverity()));
        }
        compareField(diffs, "name", dom.getName(), stax.getName());
        compareField(diffs, "category", dom.getCategory(), stax.getCategory());
        compareField(diffs, "cweId", dom.getCweId(), stax.getCweId());
        compareField(diffs, "sessionUrl", dom.getSessionUrl(), stax.getSessionUrl());
        compareField(diffs, "summary", dom.getSummary(), stax.getSummary());
        if (!Objects.equals(dom.getReproStepUrls(), stax.getReproStepUrls())) {
            diffs.add(String.format("reproStepUrls: DOM=%s vs Streaming=%s",
                    dom.getReproStepUrls(), stax.getReproStepUrls()));
        }
        return diffs;
    }

    private static void compareField(List<String> diffs, String fieldName, String domVal, String staxVal) {
        if (!Objects.equals(domVal, staxVal)) {
            diffs.add(String.format("%s: DOM='%s' vs Streaming='%s'", fieldName, domVal, staxVal));
        }
    }
}
