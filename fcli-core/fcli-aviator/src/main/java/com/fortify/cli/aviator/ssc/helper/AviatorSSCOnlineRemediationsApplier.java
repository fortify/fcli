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

import java.io.IOException;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheApplyHelper;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator._common.util.AviatorTempFprFile;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.IAviatorLogger;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;

/**
 * Online (non-cache) SSC apply-remediations: download each audited FPR to a managed host temp
 * path (required by {@link FprHandle}), apply remediations, delete the temp file.
 */
public final class AviatorSSCOnlineRemediationsApplier {
    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCOnlineRemediationsApplier.class);

    private final UnirestInstance unirest;
    private final IAviatorLogger logger;
    private final IProgressWriter progressWriter;
    private final String sourceCodeDirectory;
    private final Set<String> issueIdFilter;

    public AviatorSSCOnlineRemediationsApplier(
            UnirestInstance unirest,
            IAviatorLogger logger,
            IProgressWriter progressWriter,
            String sourceCodeDirectory,
            Set<String> issueIdFilter) {
        this.unirest = unirest;
        this.logger = logger;
        this.progressWriter = progressWriter;
        this.sourceCodeDirectory = sourceCodeDirectory;
        this.issueIdFilter = issueIdFilter;
    }

    public JsonNode applyAll(String appVersionId, OffsetDateTime sinceDate) {
        List<SSCArtifactDescriptor> artifacts =
                SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);
        BatchResult batch = processBatch(artifacts);
        RemediationMetric aggregated = AviatorRemediationMetricsHelper.aggregateMetrics(
                issueIdFilter, batch.metrics());
        return AviatorSSCApplyRemediationsHelper.buildAggregatedResultNode(
                appVersionId,
                batch.processed(),
                batch.skipped(),
                aggregated.totalRemediations(),
                aggregated.appliedRemediations(),
                aggregated.skippedRemediations(),
                aggregated.modifiedFiles(),
                RemediationsCacheApplyHelper.actionLabel(aggregated));
    }

    public JsonNode applyOne(SSCArtifactDescriptor artifact) {
        RemediationMetric metric = downloadAndApply(artifact);
        return AviatorSSCApplyRemediationsHelper.buildResultNode(
                artifact,
                metric.totalRemediations(),
                metric.appliedRemediations(),
                metric.skippedRemediations(),
                metric.modifiedFiles(),
                RemediationsCacheApplyHelper.actionLabel(metric));
    }

    private BatchResult processBatch(List<SSCArtifactDescriptor> artifacts) {
        int processed = 0;
        int skipped = 0;
        List<RemediationMetric> metrics = new ArrayList<>();
        Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

        for (int i = 0; i < artifacts.size(); i++) {
            if (remaining != null && remaining.isEmpty()) {
                break;
            }
            SSCArtifactDescriptor ad = artifacts.get(i);
            logger.progress("Processing artifact " + (i + 1) + "/" + artifacts.size() + " (id=" + ad.getId() + ")");
            try {
                RemediationMetric metric = downloadAndApply(ad, remaining);
                metrics.add(metric);
                remaining = AviatorRemediationMetricsHelper.getRemainingIssueIds(remaining, metric);
                processed++;
            } catch (AviatorSimpleException e) {
                LOG.warn("Skipping artifact {} as {}", ad.getId(), e.getMessage());
                skipped++;
            }
        }
        return new BatchResult(processed, skipped, metrics);
    }

    private RemediationMetric downloadAndApply(SSCArtifactDescriptor artifact) {
        return downloadAndApply(artifact, issueIdFilter);
    }

    private RemediationMetric downloadAndApply(SSCArtifactDescriptor artifact, Set<String> issueFilter) {
        try (AviatorTempFprFile tempFpr = AviatorTempFprFile.create(artifact.getId())) {
            downloadArtifact(artifact, tempFpr.path());
            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            try (FprHandle fprHandle = new FprHandle(tempFpr.path())) {
                return ApplyAutoRemediationOnSource.applyRemediations(
                        fprHandle, sourceCodeDirectory, logger, issueFilter);
            } catch (IOException e) {
                throw new FcliTechnicalException(
                        "Failed to close FPR handle for SSC artifact " + artifact.getId(), e);
            }
        }
    }

    private void downloadArtifact(SSCArtifactDescriptor artifact, Path destination) {
        logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + artifact.getId() + ")");
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_ARTIFACT(artifact.getId(), true),
                destination,
                SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                progressWriter);
    }

    private record BatchResult(int processed, int skipped, List<RemediationMetric> metrics) {}
}
