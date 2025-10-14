package com.fortify.cli.aviator.audit.model;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;

import java.util.List;
import java.util.Map;

/**
 * A data-holding class that represents the complete, parsed contents of an FPR file.
 * This object is the result of the initial parsing stage and serves as the input
 * for the auditing stage.
 */
public final class ParsedFprData {
    public final Map<String, AuditIssue> auditIssueMap;
    public final List<Vulnerability> vulnerabilities;
    public final FPRInfo fprInfo;
    public final AuditProcessor auditProcessor;
    public final FVDLProcessor fvdlProcessor;

    public ParsedFprData(Map<String, AuditIssue> auditIssueMap, List<Vulnerability> vulnerabilities, FPRInfo fprInfo, AuditProcessor auditProcessor, FVDLProcessor fvdlProcessor) {
        this.auditIssueMap = auditIssueMap;
        this.vulnerabilities = vulnerabilities;
        this.fprInfo = fprInfo;
        this.auditProcessor = auditProcessor;
        this.fvdlProcessor = fvdlProcessor;
    }
}