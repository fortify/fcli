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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheManifest;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheWriter;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.aviator.ssc.cli.mixin.AviatorSSCRemediationsCacheDownloadSelectorMixin;
import com.fortify.cli.aviator.ssc.helper.SinceOptionHelper;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
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

@Command(name = "download-remediations-cache", aliases = "drc")
public class AviatorSSCDownloadRemediationsCacheCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private AviatorSSCRemediationsCacheDownloadSelectorMixin artifactSelector;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;

    @Option(names = {"-f", "--file"}, required = true, paramLabel = "<file>",
            descriptionKey = "fcli.aviator.ssc.download-remediations-cache.file")
    private File outputFile;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        artifactSelector.validate();
        Path destination = outputFile.toPath();
        if (Files.exists(destination)) {
            requireConfirmation.checkConfirmed(destination);
        }

        OffsetDateTime sinceDate = SinceOptionHelper.parse(artifactSelector.getSince());
        List<SSCArtifactDescriptor> artifacts = resolveArtifacts(unirest, sinceDate);
        Map<String, String> selection = buildSelectionMetadata(unirest, sinceDate);

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create();
                RemediationsCacheWriter cacheWriter = RemediationsCacheWriter.create(
                        destination, RemediationsCacheConstants.PRODUCT_SSC, selection)) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            for (SSCArtifactDescriptor artifact : artifacts) {
                cacheWriter.addFpr(artifact.getId(), null, artifact.getUploadDate(), entryPath ->
                        downloadArtifact(unirest, artifact, entryPath, logger, progressWriter));
            }
            RemediationsCacheManifest manifest = cacheWriter.finish();
            return buildResultNode(destination, manifest);
        }
    }

    private List<SSCArtifactDescriptor> resolveArtifacts(UnirestInstance unirest, OffsetDateTime sinceDate) {
        if (artifactSelector.isAllSelected()) {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            return SSCArtifactHelper.getAllAviatorArtifacts(unirest, appVersionId, sinceDate);
        }
        if (artifactSelector.isLatestSelected()) {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            return List.of(SSCArtifactHelper.getLatestAviatorArtifact(unirest, appVersionId, sinceDate));
        }
        return List.of(SSCArtifactHelper.requireAviatorArtifact(
                SSCArtifactHelper.getArtifactDescriptor(unirest, artifactSelector.getArtifactId())));
    }

    private Map<String, String> buildSelectionMetadata(UnirestInstance unirest, OffsetDateTime sinceDate) {
        Map<String, String> selection = new LinkedHashMap<>();
        selection.put("mode", artifactSelector.getSelectionMode());
        if (artifactSelector.isArtifactIdSelected()) {
            selection.put("artifactId", artifactSelector.getArtifactId());
        } else {
            String appVersionId = artifactSelector.getAppVersionId(unirest);
            if (appVersionId != null) {
                selection.put("appVersionId", appVersionId);
            }
        }
        if (sinceDate != null) {
            selection.put("since", sinceDate.toString());
        }
        return selection;
    }

    private void downloadArtifact(UnirestInstance unirest, SSCArtifactDescriptor artifact, Path destination,
            AviatorLoggerImpl logger, IProgressWriter progressWriter) {
        logger.progress("Status: Downloading Audited FPR from SSC (artifact id=" + artifact.getId() + ")");
        SSCFileTransferHelper.download(
                unirest,
                SSCUrls.DOWNLOAD_ARTIFACT(artifact.getId(), true),
                destination,
                SSCFileTransferHelper.ISSCAddDownloadTokenFunction.ROUTEPARAM_DOWNLOADTOKEN,
                progressWriter);
    }

    private ObjectNode buildResultNode(Path destination, RemediationsCacheManifest manifest) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("file", destination.toString());
        result.put("artifactsDownloaded", manifest.getEntries().size());
        ArrayNode artifactIds = result.putArray("artifactIds");
        for (var entry : manifest.getEntries()) {
            if (entry.getArtifactId() != null) {
                artifactIds.add(entry.getArtifactId());
            }
        }
        return result;
    }

    @Override
    public String getActionCommandResult() {
        return "REMEDIATIONS_CACHE_DOWNLOADED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
