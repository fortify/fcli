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
package com.fortify.cli.aviator.applyRemediation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.exception.AviatorTechnicalException;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;


public class ApplyAutoRemediationOnSource {
    private static final Logger LOG = LoggerFactory.getLogger(ApplyAutoRemediationOnSource.class);

    public static RemediationMetric applyRemediations(FprHandle fprHandle, String sourceCodeDirectory, IAviatorLogger logger)
            throws AviatorSimpleException, AviatorTechnicalException {

        LOG.info("Starting apply auto-remediation process for file: {}", fprHandle.getFprPath());

        if (!fprHandle.hasRemediations()) {
            LOG.error("FPR file does not contain remediations.xml file: {}", fprHandle.getFprPath());
            throw new AviatorSimpleException("FPR file does not contain remediations.xml file.");
        }
        LOG.info("FPR validation successful");

        RemediationProcessor remediationProcessor = new RemediationProcessor(fprHandle, sourceCodeDirectory);
        return remediationProcessor.processRemediationXML();

    }
}
