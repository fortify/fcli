/*
 * Copyright 2021-2025 Open Text.
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
package com.fortify.cli.aviator.audit.model;

import java.util.List;
import java.util.Map;

import com.fortify.cli.aviator.fpr.Vulnerability;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.FVDLProcessor;

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