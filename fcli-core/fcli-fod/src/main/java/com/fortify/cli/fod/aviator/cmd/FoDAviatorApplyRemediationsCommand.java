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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheReader;
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorLocalFprHelper;
import com.fortify.cli.aviator._common.util.AviatorRemediationMetricsHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.output.cli.cmd.AbstractOutputCommand;
import com.fortify.cli.common.output.cli.cmd.IJsonNodeSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.cli.mixin.IFoDDelimiterMixinAware;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

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
    @Mixin private FoDDelimiterMixin delimiterMixin; // Injected into sourceSelector
    @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private SourceMixin sourceSelector;

    private static final Logger LOG = LoggerFactory.getLogger(FoDAviatorApplyRemediationsCommand.class);

    @Option(names = {"--source-dir"}, descriptionKey = "fcli.fod.aviator.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",", descriptionKey = "fcli.fod.aviator.apply-remediations.issue-ids")
    private List<String> issueIds;

    /**
     * Exclusive source selection: online FoD release (via standard release resolver) or local cache zip.
     * Propagates {@link FoDDelimiterMixin} into the nested release resolver, matching FoD patterns such as
     * {@code FoDAppOrReleaseMixin}.
     */
    public static final class SourceMixin implements IFoDDelimiterMixinAware {
        @ArgGroup(exclusive = true, multiplicity = "1")
        private SourceArgGroup source = new SourceArgGroup();

        @Override
        public void setDelimiterMixin(FoDDelimiterMixin delimiterMixin) {
            if (source != null && source.online != null) {
                source.online.setDelimiterMixin(delimiterMixin);
            }
        }

        public boolean isFromCacheSelected() {
            return source != null && source.fromCache != null;
        }

        public Path getFromCache() {
            return isFromCacheSelected() ? source.fromCache : null;
        }

        public FoDReleaseDescriptor getReleaseDescriptor(UnirestInstance unirest) {
            return source.online.getReleaseDescriptor(unirest);
        }
    }

    @Getter
    static class SourceArgGroup {
        @ArgGroup(exclusive = false, multiplicity = "1")
        private OnlineReleaseArgGroup online = new OnlineReleaseArgGroup();

        @Option(names = {"--from-cache"}, required = true, paramLabel = "<zip>",
                descriptionKey = "fcli.fod.aviator.apply-remediations.from-cache")
        private Path fromCache;
    }

    /**
     * Online release branch reusing the standard FoD release option wiring/resolution.
     */
    static class OnlineReleaseArgGroup
            extends FoDReleaseByQualifiedNameOrIdResolverMixin.AbstractFoDQualifiedReleaseNameOrIdResolverMixin {
        @Option(names = {"--release", "--rel"}, required = true, paramLabel = "id|app[:ms]:rel",
                descriptionKey = "fcli.fod.release.resolver.name-or-id")
        @Getter private String qualifiedReleaseNameOrId;
    }

    @Override
    @SneakyThrows
    public JsonNode getJsonNode() {
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = getIssueIdFilter();
        validateSelection();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (sourceSelector.isFromCacheSelected()) {
                return processFromCache(logger, issueIdFilter);
            }
            UnirestInstance unirest = unirestInstanceSupplier.getUnirestInstance();
            FoDReleaseDescriptor rd = sourceSelector.getReleaseDescriptor(unirest);
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
        if (issueIds != null && !issueIds.isEmpty() && !sourceSelector.isFromCacheSelected()) {
            throw new FcliSimpleException(
                    "--issue-ids can only be used with --from-cache; create a cache with download-remediations-cache and rerun with --from-cache");
        }
    }

    @SneakyThrows
    private JsonNode processFromCache(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        Path cacheZip = sourceSelector.getFromCache();
        try (RemediationsCacheReader cacheReader = RemediationsCacheReader.open(cacheZip)) {
            CacheProcessingResult cacheResult = processCacheEntries(cacheReader, logger, issueIdFilter);
            RemediationMetric aggregatedMetric = AviatorRemediationMetricsHelper.aggregateMetrics(
                    issueIdFilter, cacheResult.metrics());
            String status = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
            return AviatorFoDApplyRemediationsHelper.buildCacheResultNode(
                    new AviatorFoDApplyRemediationsHelper.CacheResultData(
                        cacheZip,
                        cacheResult.processedEntries(),
                        cacheResult.processedReleaseIds(),
                        aggregatedMetric.totalRemediations(),
                        aggregatedMetric.appliedRemediations(),
                        aggregatedMetric.skippedRemediations(),
                        aggregatedMetric.modifiedFiles(),
                        status));
        }
    }

    private CacheProcessingResult processCacheEntries(RemediationsCacheReader cacheReader, AviatorLoggerImpl logger,
            Set<String> issueIdFilter) {
        List<Path> fprPaths = cacheReader.getOrderedFprPaths();
        List<String> allEntryPaths = cacheReader.getOrderedEntryPaths();
        List<String> allReleaseIds = cacheReader.getOrderedReleaseIds();
        AviatorLocalFprHelper.validateLocalFprs(fprPaths, "Cache FPR");

        List<RemediationMetric> metrics = new ArrayList<>();
        List<String> processedEntries = new ArrayList<>();
        List<String> processedReleaseIds = new ArrayList<>();
        Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

        for (int i = 0; i < fprPaths.size(); i++) {
            if (remaining != null && remaining.isEmpty()) {
                break;
            }
            String entryLabel = i < allEntryPaths.size() ? allEntryPaths.get(i) : fprPaths.get(i).getFileName().toString();
            RemediationMetric metric = processCacheEntry(fprPaths.get(i), entryLabel, i + 1, fprPaths.size(), logger, remaining);
            if (metric != null) {
                metrics.add(metric);
                processedEntries.add(entryLabel);
                processedReleaseIds.add(i < allReleaseIds.size() ? allReleaseIds.get(i) : "");
                remaining = AviatorRemediationMetricsHelper.getRemainingIssueIds(remaining, metric);
            }
        }
        return new CacheProcessingResult(List.copyOf(processedEntries), List.copyOf(processedReleaseIds), metrics);
    }

    private RemediationMetric processCacheEntry(Path fprPath, String entryLabel, int index, int total,
            AviatorLoggerImpl logger, Set<String> issueFilter) {
        logger.progress("Processing FPR " + index + "/" + total + " (" + entryLabel + ")");
        logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
        try (FprHandle fprHandle = new FprHandle(fprPath)) {
            return ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger, issueFilter);
        } catch (AviatorSimpleException e) {
            LOG.warn("Skipping cache entry {} as {}", entryLabel, e.getMessage());
            return null;
        } catch (IOException e) {
            throw new FcliTechnicalException("Failed to close FPR handle for cache entry " + entryLabel, e);
        }
    }

    private record CacheProcessingResult(
            List<String> processedEntries,
            List<String> processedReleaseIds,
            List<RemediationMetric> metrics) {}

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
