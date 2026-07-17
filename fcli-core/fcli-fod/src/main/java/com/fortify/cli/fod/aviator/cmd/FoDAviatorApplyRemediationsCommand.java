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
package com.fortify.cli.fod.aviator.cmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheReader;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorLocalFprHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class FoDAviatorApplyRemediationsCommand extends AbstractOutputCommand
        implements IJsonNodeSupplier, IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin;
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;

    private static final Logger LOG = LoggerFactory.getLogger(FoDAviatorApplyRemediationsCommand.class);

    @ArgGroup(exclusive = true, multiplicity = "1")
    private SourceArgGroup source;

    @Option(names = {"--source-dir"}, descriptionKey = "fcli.fod.aviator.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.fod.aviator.apply-remediations.issue-ids")
    private List<String> issueIds;

    @Getter
    static class SourceArgGroup {
        @ArgGroup(exclusive = false)
        private OnlineSource online;

        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.fod.aviator.apply-remediations.from-cache")
        private Path fromCache;
    }

    @Getter
    static class OnlineSource {
        @Option(names = {"--release", "--rel"}, required = true, paramLabel = "id|app[:ms]:rel",
                descriptionKey = "fcli.fod.release.resolver.name-or-id")
        private String qualifiedReleaseNameOrId;
    }

    @Override
    @SneakyThrows
    public JsonNode getJsonNode() {
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = getIssueIdFilter();
        validateSelection();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
            FoDReleaseDescriptor rd = FoDReleaseHelper.getReleaseDescriptor(
                    unirest, source.online.qualifiedReleaseNameOrId, delimiterMixin.getDelimiter(), true);
            return processFprRemediations(unirest, rd, logger, issueIdFilter);
        }
    }

    private void validateSourceCodeDirectory() {
        if (sourceCodeDirectory == null || sourceCodeDirectory.isBlank()) {
            throw new FcliSimpleException("--source-dir must specify a valid directory path");
        }
    }

    private Set<String> getIssueIdFilter() {
        return AviatorIssueIdFilterUtils.normalizeIssueIds(issueIds);
    }

    private void validateSelection() {
        if (issueIds != null && !issueIds.isEmpty() && !isFromCacheSelected()) {
            throw new FcliSimpleException(
                    "--issue-ids can only be used with --from-cache; create a cache with download-remediations-cache and rerun with --from-cache");
        }
    }

    private boolean isFromCacheSelected() {
        return source != null && source.fromCache != null;
    }

    @SneakyThrows
    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        Path cacheZip = source.fromCache;
        try (RemediationsCacheReader cacheReader = RemediationsCacheReader.open(cacheZip)) {
            List<Path> fprPaths = cacheReader.getOrderedFprPaths();
            List<String> allEntryPaths = cacheReader.getManifest().getEntries().stream()
                    .sorted(java.util.Comparator.comparingInt(e -> e.getOrder()))
                    .map(e -> e.getPath())
                    .toList();
            List<String> allReleaseIds = cacheReader.getManifest().getEntries().stream()
                    .sorted(java.util.Comparator.comparingInt(e -> e.getOrder()))
                    .map(e -> e.getReleaseId() != null ? e.getReleaseId() : "")
                    .toList();

            AviatorLocalFprHelper.validateLocalFprs(fprPaths, "Cache FPR");
            List<RemediationMetric> metrics = new java.util.ArrayList<>();
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
                    remaining = getRemainingIssueIds(remaining, metric);
                }
            }

            RemediationMetric aggregatedMetric = aggregateMetrics(issueIdFilter, metrics);
            String status = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
            List<String> processedEntries = allEntryPaths.subList(0, Math.min(metrics.size(), allEntryPaths.size()));
            List<String> processedReleaseIds = allReleaseIds.subList(0, Math.min(metrics.size(), allReleaseIds.size()));
            return AviatorFoDApplyRemediationsHelper.buildCacheResultNode(
                    cacheZip,
                    List.copyOf(processedEntries),
                    List.copyOf(processedReleaseIds),
                    aggregatedMetric.totalRemediations(),
                    aggregatedMetric.appliedRemediations(),
                    aggregatedMetric.skippedRemediations(),
                    aggregatedMetric.modifiedFiles(),
                    status);
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

    @SneakyThrows
    private JsonNode processFprRemediations(UnirestInstance unirest, FoDReleaseDescriptor rd, AviatorLoggerImpl logger,
            Set<String> issueIdFilter) {
        Path downloadedFprPath = null;
        try {
            logger.progress("Status: Downloading Audited FPR from FOD");
            downloadedFprPath = downloadFprFromFod(unirest, rd);

            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            try (FprHandle fprHandle = new FprHandle(downloadedFprPath)) {
                var remediationMetric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueIdFilter);
                LOG.info("Applied remediation {}", remediationMetric.appliedRemediations());
                LOG.info("Total remediation {}", remediationMetric.totalRemediations());
                String status = remediationMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
                return AviatorFoDApplyRemediationsHelper.buildResultNode(rd, remediationMetric.totalRemediations(),
                        remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(),
                        remediationMetric.modifiedFiles(), status);
            }
        } finally {
            if (downloadedFprPath != null) {
                try {
                    Files.deleteIfExists(downloadedFprPath);
                } catch (IOException e) {
                    LOG.warn("Failed to delete temporary downloaded FPR file: {}", downloadedFprPath, e);
                }
            }
        }
    }

    @SneakyThrows
    private Path downloadFprFromFod(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        Path fprPath = Files.createTempFile("aviator_" + releaseDescriptor.getReleaseId() + "_", ".fpr");
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                getScanType(), false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        var file = fprPath.toString();
        GetRequest request = getDownloadRequest(unirest, releaseDescriptor, scanDescriptor);
        int status = 202;
        int retries = 0;
        final int maxRetries = 10;
        while (status == 202 && retries < maxRetries) {
            status = request
                    .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if (status == 202) {
                retries++;
                Thread.sleep(30000L);
            }
        }
        if (status == 202) {
            Files.deleteIfExists(fprPath);
            throw new FcliSimpleException("Timed out waiting for FoD remediations FPR download to complete after "
                    + maxRetries + " retries");
        }
        if (status < 200 || status >= 300) {
            Files.deleteIfExists(fprPath);
            throw new FcliSimpleException("FoD remediations FPR download failed with HTTP status " + status
                    + " for release " + releaseDescriptor.getReleaseId());
        }
        return fprPath;
    }

    protected FoDScanType getScanType() {
        return FoDScanType.Static;
    }

    protected GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
    }

    @Override
    public boolean isSingular() {
        return true;
    }

    @Override
    public String getActionCommandResult() {
        return "Remediation-Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
