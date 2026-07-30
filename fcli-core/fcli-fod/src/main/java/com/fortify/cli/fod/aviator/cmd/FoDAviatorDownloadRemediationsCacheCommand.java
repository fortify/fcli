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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheManifest;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheWriter;
import com.fortify.cli.aviator.config.AviatorLoggerImpl;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.progress.cli.mixin.ProgressWriterFactoryMixin;
import com.fortify.cli.common.progress.helper.IProgressWriter;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.aviator.helper.FoDRemediationsFprDownloadHelper;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "download-remediations-cache", aliases = "drc")
public class FoDAviatorDownloadRemediationsCacheCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private ProgressWriterFactoryMixin progressWriterFactoryMixin;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Injected into releaseResolver
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;

    @Option(names = {"-f", "--file"}, required = true, paramLabel = "<file>")
    private File outputFile;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        Path destination = outputFile.toPath();
        if (Files.exists(destination)) {
            requireConfirmation.checkConfirmed(destination);
        }

        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        Map<String, String> selection = new LinkedHashMap<>();
        selection.put("mode", "release");
        selection.put("releaseId", releaseDescriptor.getReleaseId());

        try (IProgressWriter progressWriter = progressWriterFactoryMixin.create();
                RemediationsCacheWriter cacheWriter = RemediationsCacheWriter.create(
                        destination, RemediationsCacheConstants.PRODUCT_FOD, selection)) {
            AviatorLoggerImpl logger = new AviatorLoggerImpl(progressWriter);
            logger.progress("Status: Downloading Audited FPR from FoD (release id="
                    + releaseDescriptor.getReleaseId() + ")");
            cacheWriter.addFodFpr(releaseDescriptor.getReleaseId(), entryPath ->
                    FoDRemediationsFprDownloadHelper.downloadStaticRemediationsFpr(unirest, releaseDescriptor, entryPath));
            logger.progress("Status: Writing remediations cache to " + destination);
            // close() writes manifest and publishes.
            RemediationsCacheManifest manifest = cacheWriter.getManifest();
            return buildResultNode(destination, releaseDescriptor, manifest);
        }
    }

    private ObjectNode buildResultNode(Path destination, FoDReleaseDescriptor releaseDescriptor, RemediationsCacheManifest manifest) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("file", destination.toString());
        result.put("releasesDownloaded", manifest.getEntries().size());
        result.putArray("releaseIds").add(releaseDescriptor.getReleaseId());
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
