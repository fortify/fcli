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
package com.fortify.cli.fod.aviator.helper;

import java.io.IOException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheApplyHelper;
import com.fortify.cli.aviator._common.util.AviatorTempFprFile;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;

/**
 * Online (non-cache) FoD apply-remediations: download audited FPR to a managed host temp path
 * (required by {@link FprHandle}), apply remediations, delete the temp file.
 */
public final class AviatorFoDOnlineRemediationsApplier {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorFoDOnlineRemediationsApplier.class);

    private AviatorFoDOnlineRemediationsApplier() {}

    public static JsonNode apply(
            UnirestInstance unirest,
            FoDReleaseDescriptor releaseDescriptor,
            String sourceCodeDirectory,
            IAviatorLogger logger,
            Set<String> issueIdFilter) {
        try (AviatorTempFprFile tempFpr = AviatorTempFprFile.create(releaseDescriptor.getReleaseId())) {
            logger.progress("Status: Downloading Audited FPR from FOD");
            FoDRemediationsFprDownloadHelper.downloadStaticRemediationsFpr(
                    unirest, releaseDescriptor, tempFpr.path());

            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            try (FprHandle fprHandle = new FprHandle(tempFpr.path())) {
                RemediationMetric metric = ApplyAutoRemediationOnSource.applyRemediations(
                        fprHandle, sourceCodeDirectory, logger, issueIdFilter);
                LOG.info("Applied remediation {}", metric.appliedRemediations());
                LOG.info("Total remediation {}", metric.totalRemediations());
                return AviatorFoDApplyRemediationsHelper.buildResultNode(
                        releaseDescriptor,
                        metric.totalRemediations(),
                        metric.appliedRemediations(),
                        metric.skippedRemediations(),
                        metric.modifiedFiles(),
                        RemediationsCacheApplyHelper.actionLabel(metric));
            } catch (IOException e) {
                throw new FcliTechnicalException(
                        "Failed to close FPR handle for FoD release " + releaseDescriptor.getReleaseId(), e);
            }
        }
    }
}
