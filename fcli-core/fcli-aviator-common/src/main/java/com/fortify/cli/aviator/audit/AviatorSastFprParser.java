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
package com.fortify.cli.aviator.audit;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fortify.cli.aviator.audit.model.ParsedFprData;
import com.fortify.cli.aviator.audit.processor.AuditProcessor;
import com.fortify.cli.aviator.audit.processor.AviatorFilterTemplateCustomizer;
import com.fortify.cli.aviator.util.Constants;
import com.fortify.cli.common.fpr.AuditTagConfig;
import com.fortify.cli.common.fpr.SastFprData;
import com.fortify.cli.common.fpr.SastFprParser;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.ISourceDecoder;

/**
 * Aviator-specific entry point for parsing a SAST FPR. Runs the product-neutral
 * {@link SastFprParser} with Aviator tag handling and wraps the result together with the
 * {@link AuditProcessor} needed for saving audit changes.
 */
public final class AviatorSastFprParser {
    private static final AuditTagConfig AVIATOR_TAG_CONFIG = new AuditTagConfig(
            List.of(Constants.AVIATOR_STATUS_TAG_ID), Set.of(Constants.AVIATOR_EXPECTED_OUTCOME_TAG_ID));

    private AviatorSastFprParser() {}

    public static ParsedFprData parse(FprHandle fprHandle, ISourceDecoder sourceDecoder) {
        Objects.requireNonNull(fprHandle, "fprHandle");
        Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        AviatorFilterTemplateCustomizer customizer = new AviatorFilterTemplateCustomizer();
        SastFprData data = SastFprParser.parse(fprHandle, sourceDecoder, customizer, AVIATOR_TAG_CONFIG);
        AuditProcessor auditProcessor = new AuditProcessor(fprHandle, sourceDecoder, data.auditDocument(),
                data.auditIssueMap(), customizer.getFilterTemplateDocument());
        return new ParsedFprData(data.auditIssueMap(), data.vulnerabilities(), data.fprInfo(),
                auditProcessor, data.streamingFVDLProcessor());
    }
}
