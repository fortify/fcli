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
package com.fortify.cli.common.fpr;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;

import com.fortify.cli.common.fpr.model.AuditIssue;
import com.fortify.cli.common.fpr.model.FPRInfo;
import com.fortify.cli.common.fpr.processor.StreamingFVDLProcessor;

/**
 * Product-neutral result of parsing a SAST FPR.
 *
 * @param auditDocument           the parsed (or default-created) {@code audit.xml} DOM
 * @param auditIssueMap           audit issues keyed by instance id
 * @param vulnerabilities         vulnerabilities from {@code audit.fvdl} with audit data applied
 * @param fprInfo                 FPR metadata, including the filter template if present
 * @param streamingFVDLProcessor  the processor that parsed {@code audit.fvdl}, for follow-up lookups
 */
public record SastFprData(
        Document auditDocument,
        Map<String, AuditIssue> auditIssueMap,
        List<Vulnerability> vulnerabilities,
        FPRInfo fprInfo,
        StreamingFVDLProcessor streamingFVDLProcessor) {}
