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
package com.fortify.cli.aviator.fpr;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fortify.cli.aviator.audit.model.ParsedFprData;
import com.fortify.cli.aviator.fpr.model.AuditIssue;
import com.fortify.cli.aviator.fpr.model.FPRInfo;
import com.fortify.cli.aviator.fpr.processor.AuditProcessor;
import com.fortify.cli.aviator.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.util.FprHandle;

/**
 * Single entry point for parsing the contents of a SAST FPR. Combines the audit.xml
 * and the streaming audit.fvdl parsing steps so that all Aviator commands consuming
 * SAST FPR data share the same parsing sequence.
 */
public final class SastFprParser {
    private SastFprParser() {}

    /**
     * Parses the audit data and vulnerabilities from the given FPR.
     *
     * @param fprHandle     the FPR to parse
     * @param sourceDecoder decoder used when reading source files referenced by stack traces
     * @return the parsed audit issues, vulnerabilities and FPR info, along with the processors
     *         that produced them for follow-up operations like saving audit changes
     */
    public static ParsedFprData parse(FprHandle fprHandle, ISourceDecoder sourceDecoder) {
        Objects.requireNonNull(fprHandle, "fprHandle");
        Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle, sourceDecoder);
        StreamingFVDLProcessor fvdlProcessor = new StreamingFVDLProcessor(fprHandle, sourceDecoder);

        Map<String, AuditIssue> auditIssueMap = auditProcessor.processAuditXML();
        FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditIssueMap, auditProcessor);
        List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);
        FPRInfo fprInfo = fprProcessor.getFprInfo();

        return new ParsedFprData(auditIssueMap, vulnerabilities, fprInfo, auditProcessor, fvdlProcessor);
    }
}
