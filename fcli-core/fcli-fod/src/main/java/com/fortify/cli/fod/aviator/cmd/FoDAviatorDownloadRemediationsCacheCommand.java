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
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheConstants;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheManifest;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheWriter;
import com.fortify.cli.aviator._common.remediations_cache.RemediationsCacheWriter.FprSource;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.unirest.HttpHeader;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
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

@Command(name = "download-remediations-cache", aliases = "drc")
public class FoDAviatorDownloadRemediationsCacheCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    private static final int MAX_RETRIES = 10;

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin;
    @Mixin private CommonOptionMixins.RequireConfirmation requireConfirmation;

    @ArgGroup(exclusive = false, multiplicity = "1")
    private FoDAviatorRemediationSelectorArgGroups.ReleaseArgGroup releaseSelection;

    @Option(names = {"-f", "--file"}, required = true, paramLabel = "<file>",
            descriptionKey = "fcli.fod.aviator.download-remediations-cache.file")
    private File outputFile;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        Path destination = outputFile.toPath();
        if (Files.exists(destination)) {
            requireConfirmation.checkConfirmed(destination);
        }

        FoDReleaseDescriptor releaseDescriptor = FoDReleaseHelper.getReleaseDescriptor(
            unirest, releaseSelection.getQualifiedReleaseNameOrId(), delimiterMixin.getDelimiter(), true);
        Path tempFpr = Files.createTempFile("aviator-cache-" + releaseDescriptor.getReleaseId() + "-", ".fpr");
        try {
            downloadFpr(unirest, releaseDescriptor, tempFpr);
            Map<String, String> selection = new LinkedHashMap<>();
            selection.put("mode", "release");
            selection.put("releaseId", releaseDescriptor.getReleaseId());
            RemediationsCacheManifest manifest = RemediationsCacheWriter.write(
                    destination,
                    RemediationsCacheConstants.PRODUCT_FOD,
                    selection,
                    List.of(FprSource.forFod(tempFpr, releaseDescriptor.getReleaseId())));
            return buildResultNode(destination, releaseDescriptor, manifest);
        } finally {
            Files.deleteIfExists(tempFpr);
        }
    }

    @SneakyThrows
    private void downloadFpr(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, Path destination) {
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                FoDScanType.Static, false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        GetRequest request = getDownloadRequest(unirest, releaseDescriptor, scanDescriptor);

        int status = 202;
        int retries = 0;
        while (status == 202 && retries < MAX_RETRIES) {
            status = request
                    .asFile(destination.toString(), StandardCopyOption.REPLACE_EXISTING)
                    .getStatus();
            if (status == 202) {
                retries++;
                Thread.sleep(30000L);
            }
        }
        if (status == 202) {
            Files.deleteIfExists(destination);
            throw new FcliSimpleException("Timed out waiting for FoD remediations FPR download to complete after "
                    + MAX_RETRIES + " retries");
        }
        if (status < 200 || status >= 300) {
            Files.deleteIfExists(destination);
            throw new FcliSimpleException("FoD remediations FPR download failed with HTTP status " + status
                    + " for release " + releaseDescriptor.getReleaseId());
        }
    }

    private GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
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
