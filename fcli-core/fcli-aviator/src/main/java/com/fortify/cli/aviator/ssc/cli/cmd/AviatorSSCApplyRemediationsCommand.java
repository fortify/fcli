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
package com.fortify.cli.aviator.ssc.cli.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorLocalFprHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsArtifactSelectorMixin;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractSSCJsonNodeOutputCommand  implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCApplyRemediationsArtifactSelectorMixin artifactSelector;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.aviator.ssc.apply-remediations.issue-ids")
    private List<String> issueIds;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        artifactSelector.validate();
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = getIssueIdFilter();
        validateIssueIdFilterMode();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            ArtifactProcessor processor = new ArtifactProcessor(unirest, logger, progressWriter, issueIdFilter);

            if (artifactSelector.isLocalFprSelected()) {
                return processor.processLocalFprRemediations(artifactSelector.getFprPaths());
            }
            OffsetDateTime sinceDate = SinceOptionHelper.parse(artifactSelector.getSince());
            if (artifactSelector.isAllOpenIssuesSelected()) {
                return processor.processAllAviatorArtifacts(sinceDate);
            }
            SSCArtifactDescriptor ad = resolveArtifactDescriptor(unirest, sinceDate);
            return processor.processFprRemediations(ad);
        }
    }

    private SSCArtifactDescriptor resolveArtifactDescriptor(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (artifactSelector.isLatestSelected()) {
            return getLatestAviatorArtifact(unirest, sinceDate);
        } else {
            return SSCArtifactHelper.getArtifactDescriptor(unirest, artifactSelector.getArtifactId());
        }
    }

    private SSCArtifactDescriptor getLatestAviatorArtifact(UnirestInstance unirest, OffsetDateTime sinceDate) {
        String appVersionId = artifactSelector.getAppVersionId(unirest);
        return SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate);
    }

    private void validateSourceCodeDirectory() {
        if (sourceCodeDirectory == null || sourceCodeDirectory.isBlank()) {
            throw new FcliSimpleException("--source-dir must specify a valid directory path");
        }
    }

    private Set<String> getIssueIdFilter() {
        return AviatorIssueIdFilterUtils.normalizeIssueIds(issueIds);
    }

    private void validateIssueIdFilterMode() {
        if (issueIds != null && !issueIds.isEmpty() && !artifactSelector.isLocalFprSelected()) {
            throw new FcliSimpleException("--issue-ids can only be used with --fpr; download the FPR once with download-remediations-fpr and rerun with --fpr");
        }
    }

    static RemediationMetric aggregateMetrics(Set<String> requestedIssueIds, Collection<RemediationMetric> metrics) {
        Set<String> modifiedFiles = new LinkedHashSet<>();
        if (requestedIssueIds == null) {
            int totalRemediations = 0;
            int appliedRemediations = 0;
            for (RemediationMetric metric : metrics) {
                totalRemediations += metric.totalRemediations();
                appliedRemediations += metric.appliedRemediations();
                modifiedFiles.addAll(metric.modifiedFiles());
            }
            return RemediationMetric.unfiltered(totalRemediations, appliedRemediations, modifiedFiles);
        }
        Set<String> appliedIssueIds = new LinkedHashSet<>();
        for (RemediationMetric metric : metrics) {
            modifiedFiles.addAll(metric.modifiedFiles());
            appliedIssueIds.addAll(metric.appliedIssueIds());
        }
        return RemediationMetric.filtered(requestedIssueIds, appliedIssueIds, modifiedFiles);
    }

    static Set<String> getRemainingIssueIds(Set<String> requestedIssueIds, RemediationMetric metric) {
        if (requestedIssueIds == null || requestedIssueIds.isEmpty()) {
            return requestedIssueIds;
        }
        Set<String> remainingIssueIds = new LinkedHashSet<>(requestedIssueIds);
        remainingIssueIds.removeAll(metric.appliedIssueIds());
        return remainingIssueIds;
    }

    /**
     * Inner class to encapsulate artifact processing logic, avoiding the need to pass
     * unirest, logger, and progressWriter through multiple method calls.
     */
    @RequiredArgsConstructor
    private class ArtifactProcessor {
        private final UnirestInstance unirest;
        private final AviatorLoggerImpl logger;
        private final IProgressWriter progressWriter;
        private final Set<String> issueIdFilter;

        @SneakyThrows
        JsonNode processAllAviatorArtifacts(OffsetDateTime sinceDate) {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            List<SSCArtifactDescriptor> artifacts = SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);
            
            ArtifactBatchResult batchResult = processBatchOfArtifacts(artifacts);
            RemediationMetric aggregatedMetric = aggregateMetrics(issueIdFilter, batchResult.metrics());
            String action = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
            return AviatorSSCApplyRemediationsHelper.buildAggregatedResultNode(
                    appVersionId, batchResult.processed(), batchResult.skipped(),
                    aggregatedMetric.totalRemediations(), aggregatedMetric.appliedRemediations(), 
                    aggregatedMetric.skippedRemediations(), aggregatedMetric.modifiedFiles(), action);
        }
        
        private ArtifactBatchResult processBatchOfArtifacts(List<SSCArtifactDescriptor> artifacts) {
            int processed = 0, skipped = 0;
            List<RemediationMetric> metrics = new java.util.ArrayList<>();
            Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);
            
            for (int i = 0; i < artifacts.size(); i++) {
                if (shouldStopProcessing(remaining)) {
                    break;
                }
                int artifactIndex = i + 1;
                SSCArtifactDescriptor ad = artifacts.get(i);
                
                ArtifactProcessResult result = processSingleArtifact(ad, artifactIndex, artifacts.size(), remaining);
                if (result.isSuccess()) {
                    metrics.add(result.metric());
                    remaining = getRemainingIssueIds(remaining, result.metric());
                    processed++;
                } else {
                    skipped++;
                }
            }
            return new ArtifactBatchResult(processed, skipped, metrics);
        }
        
        @SneakyThrows
        private ArtifactProcessResult processSingleArtifact(SSCArtifactDescriptor ad, int index, int total, Set<String> issueFilter) {
            logger.progress("Processing artifact " + index + "/" + total + " (id=" + ad.getId() + ")");
            Path fprPath = null;
            try {
                fprPath = downloadArtifactFpr(ad);
                try (FprHandle fprHandle = new FprHandle(fprPath)) {
                    RemediationMetric metric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueFilter);
                    return ArtifactProcessResult.success(metric);
                }
            } catch (AviatorSimpleException e) {
                LOG.warn("Skipping artifact {} as {}", ad.getId(), e.getMessage());
                return ArtifactProcessResult.failure();
            } finally {
                cleanupFprFile(fprPath);
            }
        }
        
        private void cleanupFprFile(Path fprPath) {
            if (fprPath != null) {
                try {
                    Files.deleteIfExists(fprPath);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary FPR file: {}", fprPath, e);
                }
            }
        }
        
        private boolean shouldStopProcessing(Set<String> remaining) {
            return remaining != null && remaining.isEmpty();
        }

        @SneakyThrows
        JsonNode processLocalFprRemediations(List<Path> fprPaths) {
            AviatorLocalFprHelper.validateLocalFprs(fprPaths);
            List<RemediationMetric> metrics = new java.util.ArrayList<>();
            Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

            for (int i = 0; i < fprPaths.size(); i++) {
                if (shouldStopProcessing(remaining)) {
                    break;
                }
                Path fprPath = fprPaths.get(i);
                logger.progress("Processing FPR " + (i + 1) + "/" + fprPaths.size() + " (" + fprPath + ")");
                logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
                try (FprHandle fprHandle = new FprHandle(fprPath)) {
                    RemediationMetric metric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, remaining);
                    metrics.add(metric);
                    remaining = getRemainingIssueIds(remaining, metric);
                }
            }

            RemediationMetric aggregatedMetric = aggregateMetrics(issueIdFilter, metrics);
            String action = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
                return AviatorSSCApplyRemediationsHelper.buildLocalFprResultNode(
                    new AviatorSSCApplyRemediationsHelper.LocalFprResultData(
                        fprPaths,
                        metrics.size(),
                        0,
                        aggregatedMetric.totalRemediations(),
                        aggregatedMetric.appliedRemediations(),
                        aggregatedMetric.skippedRemediations(),
                        aggregatedMetric.modifiedFiles(),
                        action));
        }

        @SneakyThrows
        private Path downloadArtifactFpr(SSCArtifactDescriptor ad) {
            Path fprPath = Files.createTempFile("aviator_" + ad.getId() + "_", ".fpr");
            logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + ad.getId() + ")");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
                    fprPath.toFile(),
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                    progressWriter);
            return fprPath;
        }

        @SneakyThrows
        JsonNode processFprRemediations(SSCArtifactDescriptor ad) {
            Path fprPath = downloadArtifactFpr(ad);
            try {
                logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
                try (FprHandle fprHandle = new FprHandle(fprPath)) {
                    var remediationMetric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueIdFilter);
                    String status = remediationMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
                    return AviatorSSCApplyRemediationsHelper.buildResultNode(ad, remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), remediationMetric.modifiedFiles(), status);
                }
            } finally {
                try {
                    Files.deleteIfExists(fprPath);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprPath, e);
                }
            }
        }
    }

    @Override
    public boolean isSingular() { return true; }

    @Override
    public String getActionCommandResult() {
        return "Remediations Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
    
    private record ArtifactBatchResult(int processed, int skipped, List<RemediationMetric> metrics) {}
    
    private sealed interface ArtifactProcessResult {
        static ArtifactProcessResult success(RemediationMetric metric) { return new Success(metric); }
        static ArtifactProcessResult failure() { return new Failure(); }
        
        record Success(RemediationMetric metric) implements ArtifactProcessResult {}
        record Failure() implements ArtifactProcessResult {}
        
        default boolean isSuccess() { return this instanceof Success; }
        default RemediationMetric metric() { return ((Success) this).metric(); }
    }
}
