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
import com.fortify.cli.aviator._common.util.AviatorIssueIdFilterUtils;
import com.fortify.cli.aviator._common.util.AviatorLocalFprHelper;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.fpr.processor.RemediationProcessor.RemediationMetric;
import com.fortify.cli.aviator.util.FprHandle;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod.aviator.helper.AviatorFoDApplyRemediationsHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class FoDAviatorApplyRemediationsCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.OptionalOption releaseResolver;
    private static final Logger LOG = LoggerFactory.getLogger(FoDAviatorApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}) private String sourceCodeDirectory = System.getProperty("user.dir");
    @Option(names = {"--issue-ids"}, split = ",")
    private List<String> issueIds;
    @Option(names = {"--fpr"}, arity = "1..*", paramLabel = "<file>", descriptionKey = "fcli.fod.aviator.apply-remediations.fpr")
    @DisableTest({TestType.MULTI_OPT_SPLIT, TestType.MULTI_OPT_PLURAL_NAME, TestType.OPT_ARITY_VARIABLE})
    private List<Path> fprPaths;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validateSourceCodeDirectory();
        Set<String> issueIdFilter = getIssueIdFilter();
        validateSelection();
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (isLocalFprSelected()) {
                return processLocalFprRemediations(logger, issueIdFilter);
            }
            FoDReleaseDescriptor rd = releaseResolver.getReleaseDescriptor(unirest);
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
        boolean releaseSelected = releaseResolver.getQualifiedReleaseNameOrId() != null && !releaseResolver.getQualifiedReleaseNameOrId().isBlank();
        boolean localFprSelected = isLocalFprSelected();
        if (releaseSelected == localFprSelected) {
            throw new FcliSimpleException("Exactly one of --release/--rel or --fpr must be specified");
        }
        if (issueIds != null && !issueIds.isEmpty() && !localFprSelected) {
            throw new FcliSimpleException("--issue-ids can only be used with --fpr; download the FPR once with download-remediations-fpr and rerun with --fpr");
        }
    }

    private boolean isLocalFprSelected() {
        return fprPaths != null && !fprPaths.isEmpty();
    }

    @SneakyThrows
    private JsonNode processLocalFprRemediations(AviatorLoggerImpl logger, Set<String> issueIdFilter) {
        AviatorLocalFprHelper.validateLocalFprs(fprPaths);
        List<RemediationMetric> metrics = new java.util.ArrayList<>();
        Set<String> remaining = issueIdFilter == null ? null : new LinkedHashSet<>(issueIdFilter);

        for (int i = 0; i < fprPaths.size(); i++) {
            if (remaining != null && remaining.isEmpty()) {
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
        String status = aggregatedMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
        return AviatorFoDApplyRemediationsHelper.buildLocalFprResultNode(fprPaths,
                aggregatedMetric.totalRemediations(), aggregatedMetric.appliedRemediations(),
                aggregatedMetric.skippedRemediations(), aggregatedMetric.modifiedFiles(), status);
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
                return AviatorFoDApplyRemediationsHelper.buildResultNode(rd, remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), remediationMetric.modifiedFiles(), status);
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
    private Path  downloadFprFromFod(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor) {
        Path fprPath = Files.createTempFile("aviator_" + releaseDescriptor.getReleaseId() + "_", ".fpr");
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                getScanType(), false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        var file = fprPath.toString();
        GetRequest request = getDownloadRequest(unirest, releaseDescriptor, scanDescriptor);
        int status = 202;
        while ( status==202 ) {
            status = request
                    .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return fprPath;
    }



    protected FoDScanType getScanType() {
        return FoDScanType.Static;
    }

    protected GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                // Use headerReplace to replace rather than add the Accept header (avoid duplicates with defaults)
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
