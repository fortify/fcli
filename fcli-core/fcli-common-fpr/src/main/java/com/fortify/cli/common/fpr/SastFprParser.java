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
import java.util.Objects;

import com.fortify.cli.common.fpr.processor.AuditXmlReader;
import com.fortify.cli.common.fpr.processor.FilterTemplateCustomizer;
import com.fortify.cli.common.fpr.processor.StreamingFVDLProcessor;
import com.fortify.cli.common.fpr.util.FprHandle;
import com.fortify.cli.common.fpr.utils.ISourceDecoder;

/**
 * Single entry point for parsing the contents of a SAST FPR. Combines the audit.xml
 * and the streaming audit.fvdl parsing steps so that all consumers share the same
 * parsing sequence. This class is product-neutral; product-specific behavior is
 * supplied through {@link FilterTemplateCustomizer} and {@link AuditTagConfig}.
 */
public final class SastFprParser {
    private SastFprParser() {}

    /** Parses the FPR without any product-specific customization. */
    public static SastFprData parse(FprHandle fprHandle, ISourceDecoder sourceDecoder) {
        return parse(fprHandle, sourceDecoder, FilterTemplateCustomizer.NONE, AuditTagConfig.DEFAULT);
    }

    /**
     * Parses the audit data and vulnerabilities from the given FPR.
     *
     * @param fprHandle      the FPR to parse
     * @param sourceDecoder  decoder used when reading source files referenced by stack traces
     * @param customizer     hook to adjust the parsed filter template
     * @param tagConfig      product-specific audit tag configuration
     */
    public static SastFprData parse(FprHandle fprHandle, ISourceDecoder sourceDecoder,
            FilterTemplateCustomizer customizer, AuditTagConfig tagConfig) {
        Objects.requireNonNull(fprHandle, "fprHandle");
        Objects.requireNonNull(sourceDecoder, "sourceDecoder");
        Objects.requireNonNull(customizer, "customizer");
        Objects.requireNonNull(tagConfig, "tagConfig");
        StreamingFVDLProcessor fvdlProcessor = new StreamingFVDLProcessor(fprHandle, sourceDecoder);

        AuditXmlReader.AuditXmlContent auditContent = new AuditXmlReader().read(fprHandle);
        FPRProcessor fprProcessor = new FPRProcessor(fprHandle, auditContent.auditIssues(), customizer, tagConfig);
        List<Vulnerability> vulnerabilities = fprProcessor.process(fvdlProcessor);

        return new SastFprData(auditContent.document(), auditContent.auditIssues(), vulnerabilities,
                fprProcessor.getFprInfo(), fvdlProcessor);
    }
}
