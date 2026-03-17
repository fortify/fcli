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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;
import com.fortify.cli.aviator.applyRemediation.ApplyAutoRemediationOnSource;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
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
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "apply-remediations")
public class AviatorSSCApplyRemediationsCommand extends AbstractSSCJsonNodeOutputCommand  implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private SSCAppVersionResolverMixin.OptionalOption appVersionResolver;

    @Option(names = {"--artifact-id"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.artifact-id")
    private String artifactId;

    @Option(names = {"--latest"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.latest")
    private boolean latest;

    @Option(names = {"--all-open-issues"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.all-open-issues")
    private boolean allOpenIssues;

    @Option(names = {"--since"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.since")
    private String since;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        validateOptions();
        validateSourceCodeDirectory();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(since);
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (allOpenIssues) {
                return processAllAviatorArtifacts(unirest, logger, sinceDate);
            }
            SSCArtifactDescriptor ad = resolveArtifactDescriptor(unirest, sinceDate);
            return processFprRemediations(unirest, ad, logger);
        }
    }

    private void validateOptions() {
        boolean hasArtifactId = artifactId != null && !artifactId.isBlank();
        boolean hasSince = since != null && !since.isBlank();
        int optionCount = (hasArtifactId ? 1 : 0) + (latest ? 1 : 0) + (allOpenIssues ? 1 : 0);

        if (optionCount > 1) {
            throw new FcliSimpleException("--artifact-id, --latest, and --all-open-issues are mutually exclusive");
        }
        if (optionCount == 0) {
            throw new FcliSimpleException("One of --artifact-id, --latest, or --all-open-issues must be specified");
        }
        if ((latest || allOpenIssues) && appVersionResolver.getAppVersionNameOrId() == null) {
            throw new FcliSimpleException("--av/--appversion is required when using --latest or --all-open-issues");
        }
        if (hasSince && hasArtifactId) {
            throw new FcliSimpleException("--since cannot be used with --artifact-id; use --latest or --all-open-issues");
        }
        if (hasSince && !latest && !allOpenIssues) {
            throw new FcliSimpleException("--since can only be used with --latest or --all-open-issues");
        }
    }

    private SSCArtifactDescriptor resolveArtifactDescriptor(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (latest) {
            return getLatestAviatorArtifact(unirest, sinceDate);
        } else {
            return SSCArtifactHelper.getArtifactDescriptor(unirest, artifactId);
        }
    }

    private SSCArtifactDescriptor getLatestAviatorArtifact(UnirestInstance unirest, OffsetDateTime sinceDate) {
        String appVersionId = appVersionResolver.getAppVersionId(unirest);
        return SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate);
    }

    private void validateSourceCodeDirectory() {
        if (sourceCodeDirectory == null || sourceCodeDirectory.isBlank()) {
            throw new FcliSimpleException("--source-dir must specify a valid directory path");
        }
    }

    @SneakyThrows
    private JsonNode processAllAviatorArtifacts(UnirestInstance unirest, AviatorLoggerImpl logger, OffsetDateTime sinceDate) {
        String appVersionId = appVersionResolver.getAppVersionId(unirest);
        List<SSCArtifactDescriptor> artifacts = SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);

        int totalRemediations = 0, appliedRemediations = 0, skippedRemediations = 0;
        int artifactsProcessed = 0, artifactsSkipped = 0;

        for (SSCArtifactDescriptor ad : artifacts) {
            int artifactIndex = artifactsProcessed + artifactsSkipped + 1;
            logger.progress("Processing artifact " + artifactIndex + "/" + artifacts.size() + " (id=" + ad.getId() + ")");
            Path fprPath = null;
            try {
                fprPath = downloadArtifactFpr(unirest, ad, logger);
                try (FprHandle fprHandle = new FprHandle(fprPath)) {
                    var metric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger);
                    totalRemediations   += metric.totalRemediations();
                    appliedRemediations += metric.appliedRemediations();
                    skippedRemediations += metric.skippedRemediations();
                    artifactsProcessed++;
                }
            } catch (AviatorSimpleException e) {
                LOG.warn("Skipping artifact {} as {}", ad.getId(), e.getMessage());
                artifactsSkipped++;
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

        String action = appliedRemediations > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
        return AviatorSSCApplyRemediationsHelper.buildAggregatedResultNode(
                appVersionId, artifactsProcessed, artifactsSkipped,
                totalRemediations, appliedRemediations, skippedRemediations, action);
    }

    @SneakyThrows
    private Path downloadArtifactFpr(UnirestInstance unirest, SSCArtifactDescriptor ad, AviatorLoggerImpl logger) {
        Path fprPath = Files.createTempFile("aviator_" + ad.getId() + "_", ".fpr");
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + ad.getId() + ")");
            SSCFileTransferHelper.download(
                    unirest,
                    SSCUrls.DOWNLOAD_ARTIFACT(ad.getId(), true),
                    fprPath.toFile(),
                    SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                    progressWriter);
        }
        return fprPath;
    }

    @SneakyThrows
    private JsonNode processFprRemediations(UnirestInstance unirest, SSCArtifactDescriptor ad, AviatorLoggerImpl logger) {
        Path fprPath = downloadArtifactFpr(unirest, ad, logger);
        try {
            logger.progress("Status: Processing FPR with Aviator for Applying Auto Remediations");
            try (FprHandle fprHandle = new FprHandle(fprPath)) {
                var remediationMetric = ApplyAutoRemediationOnSource.applyRemediations(fprHandle, sourceCodeDirectory, logger);
                String status = remediationMetric.appliedRemediations() > 0 ? "Remediation-Applied" : "No-Remediation-Applied";
                return AviatorSSCApplyRemediationsHelper.buildResultNode(ad, remediationMetric.totalRemediations(), remediationMetric.appliedRemediations(), remediationMetric.skippedRemediations(), status);
            }
        } finally {
            try {
                Files.deleteIfExists(fprPath);
            } catch (IOException e) {
                LOG.warn("Failed to delete temporary downloaded FPR file: {}", fprPath, e);
            }
        }
    }

    @Override
    public boolean isSingular() { return !allOpenIssues; }

    @Override
    public String getActionCommandResult() {
        return "Remediations Applied";
    }

    @Override
    public JsonNode transformRecord(JsonNode record) {
        return record;
    }
}
