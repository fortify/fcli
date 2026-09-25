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
package com.fortify.cli.aviator.applyRemediation;

import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.remediation.RemediationExecutionMode;
import com.fortify.cli.aviator.fpr.remediation.RemediationProcessingOptions;
import com.fortify.cli.aviator.fpr.remediation.RemediationProcessor;
import com.fortify.cli.aviator.fpr.remediation.model.RemediationMetric;
import com.fortify.cli.aviator.fpr.utils.ISourceDecoder;
import com.fortify.cli.aviator.fpr.utils.SourceDecoders;
import com.fortify.cli.aviator.util.FprHandle;

public class ApplyAutoRemediationOnSource {
    private static final Logger LOG = LoggerFactory.getLogger(ApplyAutoRemediationOnSource.class);

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory)
            throws AviatorSimpleException, AviatorTechnicalException {
        return applyRemediations(fprHandle, sourceCodeDirectory, SourceDecoders.defaults());
    }

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory,
            ISourceDecoder sourceDecoder)
            throws AviatorSimpleException, AviatorTechnicalException {
        return applyRemediations(fprHandle, sourceCodeDirectory, null,
            new RemediationProcessingOptions(Set.of(), RemediationExecutionMode.APPLY, sourceDecoder));
    }

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger,
            Set<String> issueIdFilter)
            throws AviatorSimpleException, AviatorTechnicalException {
        return applyRemediations(fprHandle, sourceCodeDirectory, logger, issueIdFilter, false, SourceDecoders.defaults());
    }

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger,
            Set<String> issueIdFilter, boolean previewMode)
            throws AviatorSimpleException, AviatorTechnicalException {
        return applyRemediations(fprHandle, sourceCodeDirectory, logger, issueIdFilter, previewMode, SourceDecoders.defaults());
    }

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger,
            Set<String> issueIdFilter, boolean previewMode, ISourceDecoder sourceDecoder)
            throws AviatorSimpleException, AviatorTechnicalException {
        RemediationExecutionMode executionMode = previewMode ? RemediationExecutionMode.PREVIEW : RemediationExecutionMode.APPLY;
        return applyRemediations(fprHandle, sourceCodeDirectory, logger,
            new RemediationProcessingOptions(issueIdFilter, executionMode, sourceDecoder));
    }

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger,
            RemediationProcessingOptions options)
            throws AviatorSimpleException, AviatorTechnicalException {
        Objects.requireNonNull(options, "options");
        LOG.info("Starting {} process for file: {}",
            options.isPreview() ? "preview" : "apply auto-remediation", fprHandle.getFprPath());
        if (!fprHandle.hasRemediations()) {
            throw new AviatorSimpleException("FPR file does not contain remediations.xml file.");
        }
        LOG.info("FPR validation successful");

        RemediationProcessor remediationProcessor = new RemediationProcessor(fprHandle, sourceCodeDirectory, options);
        return remediationProcessor.processRemediationXML();
    }
}
