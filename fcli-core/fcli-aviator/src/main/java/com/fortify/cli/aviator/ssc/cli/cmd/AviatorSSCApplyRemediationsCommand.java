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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheEntry;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheReader;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorLocalFprHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCApplyRemediationsSourceMixin;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCApplyRemediationsHelper;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.cli.mixin.SSCUnirestInstanceSupplierMixin;
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
public class AviatorSSCApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCApplyRemediationsSourceMixin sourceSelector;
    @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.aviator.ssc.apply-remediations.issue-ids")
    private List<String> issueIds;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode() {
        sourceSelector.validate();
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = getIssueIdFilter();
        validateIssueIdFilterMode();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (sourceSelector.isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
            ArtifactProcessor processor = new ArtifactProcessor(unirest, logger, progressWriter, issueIdFilter);
            OffsetDateTime sinceDate = SinceOptionHelper.parse(sourceSelector.getSince());
            if (sourceSelector.isAllSelected()) {
                return processor.processAllAviatorArtifacts(sinceDate);
            }
            SSCArtifactDescriptor ad = resolveArtifactDescriptor(unirest, sinceDate);
            return processor.processFprRemediations(ad);
        }
    }

    @SneakyThrows
    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        Path cacheZip = sourceSelector.getFromCache();
        try (RemediationsCacheReader cacheReader = RemediationsCacheReader.open(cacheZip)) {
            List<Path> fprPaths = cacheReader.getOrderedFprPaths();
            List<String> allEntryPaths = orderedEntryPaths(cacheReader);
            List<String> allArtifactIds = orderedArtifactIds(cacheReader);
            String appVersionId = cacheReader.getManifest().getSelection() != null
                    ? cacheReader.getManifest().getSelection().get("appVersionId")
                    : null;

            AviatorLocalFprHelper.validateLocalFprs(fprPaths, "Cache FPR");
            List<RemediationMetric> metrics = new ArrayList<>();
            List<String> processedEntries = new ArrayList<>();
            List<String> processedArtifactIds = new ArrayList<>();
            int skipped = 0;
            Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

            for (int i = 0; i < fprPaths.size(); i++) {
                if (remaining != null && remaining.isEmpty()) {
                    break;
                }
                Path fprPath = fprPaths.get(i);
                String entryLabel = i < allEntryPaths.size() ? allEntryPaths.get(i) : fprPath.getFileName().toString();
                logger.progress("Processing FPR " + (i + 1) + "/" + fprPaths.size() + " (" + entryLabel + ")");
                logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
                try (FprHandle fprHandle = new FprHandle(fprPath)) {
                    RemediationMetric metric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, remaining);
                    metrics.add(metric);
                    processedEntries.add(entryLabel);
                    processedArtifactIds.add(i < allArtifactIds.size() ? allArtifactIds.get(i) : "");
                    remaining = getRemainingIssueIds(remaining, metric);
                } catch (AviatorSimpleException e) {
                    LOG.warn("Skipping cache entry {} as {}", entryLabel, e.getMessage());
                    skipped++;
                }
            }

            RemediationMetric aggregatedMetric = aggregateMetrics(issueIdFilter, metrics);
            String action = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
            return AviatorSSCApplyRemediationsHelper.buildCacheResultNode(
                    new AviatorSSCApplyRemediationsHelper.CacheResultData(
                            cacheZip,
                            List.copyOf(processedEntries),
                            List.copyOf(processedArtifactIds),
                            appVersionId,
                            metrics.size(),
                            skipped,
                            aggregatedMetric.totalRemediations(),
                            aggregatedMetric.appliedRemediations(),
                            aggregatedMetric.skippedRemediations(),
                            aggregatedMetric.modifiedFiles(),
                            action));
        }
    }

    private static List<String> orderedEntryPaths(RemediationsCacheReader cacheReader) {
        return cacheReader.getManifest().getEntries().stream()
                .sorted(java.util.Comparator.comparingInt(RemediationsCacheEntry::getOrder))
                .map(RemediationsCacheEntry::getPath)
                .toList();
    }

    private static List<String> orderedArtifactIds(RemediationsCacheReader cacheReader) {
        return cacheReader.getManifest().getEntries().stream()
                .sorted(java.util.Comparator.comparingInt(RemediationsCacheEntry::getOrder))
                .map(e -> e.getArtifactId() != null ? e.getArtifactId() : "")
                .toList();
    }

    private SSCArtifactDescriptor resolveArtifactDescriptor(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (sourceSelector.isLatestSelected()) {
            String appVersionId = sourceSelector.getAppVersionId(unirest);
            return SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate);
        }
        return SSCArtifactHelper.getArtifactDescriptor(unirest, sourceSelector.getArtifactId());
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
        if (issueIds != null && !issueIds.isEmpty() && !sourceSelector.isFromCacheSelected()) {
            throw new FcliSimpleException(
                    "--issue-ids can only be used with --from-cache; create a cache with download-remediations-cache and rerun with --from-cache");
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

    @RequiredArgsConstructor
    private class ArtifactProcessor {
        private final UnirestInstance unirest;
        private final AviatorLoggerImpl logger;
        private final IProgressWriter progressWriter;
        private final Set<String> issueIdFilter;

        @SneakyThrows
        JsonNode processAllAviatorArtifacts(OffsetDateTime sinceDate) {
            String appVersionId = sourceSelector.getAppVersionId(unirest);
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
                if (remaining != null && remaining.isEmpty()) {
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
                if (fprPath != null) {
                    try {
                        Files.deleteIfExists(fprPath);
                    } catch (IOException e) {
                        LOG.warn("Failed to delete temporary FPR file: {}", fprPath, e);
                    }
                }
            }
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
                    return AviatorSSCApplyRemediationsHelper.buildResultNode(ad, remediationMetric.totalRemediations(),
                            remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(),
                            remediationMetric.modifiedFiles(), status);
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
    public boolean isSingular() {
        return true;
    }

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
        static ArtifactProcessResult success(RemediationMetric metric) {
            return new Success(metric);
        }

        static ArtifactProcessResult failure() {
            return new Failure();
        }

        record Success(RemediationMetric metric) implements ArtifactProcessResult {}

        record Failure() implements ArtifactProcessResult {}

        default boolean isSuccess() {
            return this instanceof Success;
        }

        default RemediationMetric metric() {
            return ((Success) this).metric();
        }
    }
}
