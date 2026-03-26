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
    @Getter @Mixin private OutputHelperMixins.TableNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCApplyRemediationsArtifactSelectorMixin artifactSelector;

    private static final Logger LOG = LoggerFactory.getLogger(AviatorSSCApplyRemediationsCommand.class);
    @Option(names = {"--source-dir"}, descriptionKey = "fcli.aviator.ssc.apply-remediations.source-dir")
    private String sourceCodeDirectory = System.getProperty("user.dir");

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        artifactSelector.validate();
        validateSourceCodeDirectory();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(artifactSelector.getSince());
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            ArtifactProcessor processor = new ArtifactProcessor(unirest, logger, progressWriter);

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

    /**
     * Inner class to encapsulate artifact processing logic, avoiding the need to pass
     * unirest, logger, and progressWriter through multiple method calls.
     */
    @RequiredArgsConstructor
    private class ArtifactProcessor {
        private final UnirestInstance unirest;
        private final AviatorLoggerImpl logger;
        private final IProgressWriter progressWriter;

        @SneakyThrows
        JsonNode processAllAviatorArtifacts(OffsetDateTime sinceDate) {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            List<SSCArtifactDescriptor> artifacts = SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);

            int totalRemediations = 0, appliedRemediations = 0, skippedRemediations = 0;
            int artifactsProcessed = 0, artifactsSkipped = 0;

            for (SSCArtifactDescriptor ad : artifacts) {
                int artifactIndex = artifactsProcessed + artifactsSkipped + 1;
                logger.progress("Processing artifact " + artifactIndex + "/" + artifacts.size() + " (id=" + ad.getId() + ")");
                Path fprPath = null;
                try {
                    fprPath = downloadArtifactFpr(ad);
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
}
