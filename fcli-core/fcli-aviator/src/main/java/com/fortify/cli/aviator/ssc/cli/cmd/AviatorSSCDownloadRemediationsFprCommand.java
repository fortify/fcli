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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsFprDownloadSelectorMixin;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc._common.rest.ssc.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactDescriptor;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "download-remediations-fpr")
public class AviatorSSCDownloadRemediationsFprCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCRemediationsFprDownloadSelectorMixin artifactSelector;
    @Option(names = {"-f", "--file"}, paramLabel = "<file>", descriptionKey = "fcli.aviator.ssc.download-remediations-fpr.file")
    private File outputFile;
    @Option(names = {"--dir"}, paramLabel = "<dir>", descriptionKey = "fcli.aviator.ssc.download-remediations-fpr.dir")
    private Path outputDir;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        artifactSelector.validate();
        validateDestinationOptions();
        OffsetDateTime sinceDate = SinceOptionHelper.parse(artifactSelector.getSince());
        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create()) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            if (artifactSelector.isAllOpenIssuesSelected()) {
                return downloadAll(unirest, sinceDate, logger, progressWriter);
            }
            SSCArtifactDescriptor artifact = resolveArtifactDescriptor(unirest, sinceDate);
            Path destination = getSingleDestination(artifact);
            downloadArtifact(unirest, artifact, destination, logger, progressWriter);
            return buildSingleResultNode(artifact, destination);
        }
    }

    private SSCArtifactDescriptor resolveArtifactDescriptor(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (artifactSelector.isLatestSelected()) {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            return SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate);
        }
        return SSCArtifactHelper.requireAviatorArtifact(
                SSCArtifactHelper.getArtifactDescriptor(unirest, artifactSelector.getArtifactId()));
    }

    @SneakyThrows
    private JsonNode downloadAll(UnirestInstance unirest, OffsetDateTime sinceDate, AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        Files.createDirectories(outputDir);
        if (!Files.isDirectory(outputDir)) {
            throw new FcliSimpleException("--dir must specify a directory: " + outputDir);
        }

        String appVersionId = artifactSelector.getAppVersionId(unirest);
        List<SSCArtifactDescriptor> artifacts = SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("appVersionId", appVersionId);
        result.put("artifactsDownloaded", artifacts.size());
        ArrayNode files = result.putArray("files");
        ArrayNode artifactIds = result.putArray("artifactIds");
        for (int i = 0; i < artifacts.size(); i++) {
            SSCArtifactDescriptor artifact = artifacts.get(i);
            Path destination = outputDir.resolve(String.format("%03d_remediations_artifact_%s.fpr", i + 1, artifact.getId()));
            downloadArtifact(unirest, artifact, destination, logger, progressWriter);
            files.add(destination.toString());
            artifactIds.add(artifact.getId());
        }
        result.put(IActionCommandResultSupplier.actionFieldName, getActionCommandResult());
        return result;
    }

    private Path getSingleDestination(SSCArtifactDescriptor artifact) {
        return outputFile == null ? Path.of(String.format("remediations_artifact_%s.fpr", artifact.getId())) : outputFile.toPath();
    }

    private void validateDestinationOptions() {
        if (artifactSelector.isAllOpenIssuesSelected()) {
            if (outputFile != null) {
                throw new FcliSimpleException("-f/--file cannot be used with --all; use --dir");
            }
            if (outputDir == null) {
                throw new FcliSimpleException("--dir must be specified when using --all");
            }
        } else if (outputDir != null) {
            throw new FcliSimpleException("--dir can only be used with --all; use -f/--file for a single FPR download");
        }
    }

    private void downloadArtifact(UnirestInstance unirest, SSCArtifactDescriptor artifact, Path destination,
            AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + artifact.getId() + ")");
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_ARTIFACT(artifact.getId(), true),
                destination.toFile(),
                SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                progressWriter);
    }

    private ObjectNode buildSingleResultNode(SSCArtifactDescriptor artifact, Path destination) {
        ObjectNode result = artifact.asObjectNode();
        result.put("artifactsDownloaded", 1);
        result.putArray("artifactIds").add(artifact.getId());
        result.putArray("files").add(destination.toString());
        result.put("file", destination.toString());
        result.put(IActionCommandResultSupplier.actionFieldName, getActionCommandResult());
        return result;
    }

    @Override
    public String getActionCommandResult() {
        return "REMEDIATIONS_FPR_DOWNLOADED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}