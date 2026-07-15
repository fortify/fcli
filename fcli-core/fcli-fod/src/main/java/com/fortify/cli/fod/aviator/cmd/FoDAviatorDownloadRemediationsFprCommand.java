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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseDescriptor;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "download-remediations-fpr")
public class FoDAviatorDownloadRemediationsFprCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    private static final int MAX_RETRIES = 10;

    @Getter @Mixin private OutputHelperMixins.DetailsNoQuery outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin;
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;
    @Option(names = {"-f", "--file"}, paramLabel = "<file>", descriptionKey = "fcli.fod.aviator.download-remediations-fpr.file")
    private File outputFile;

    @Override
    @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDReleaseDescriptor releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);
        FoDScanDescriptor scanDescriptor = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                FoDScanType.Static, false);
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        Path destination = getDestination(releaseDescriptor);
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
            throw new FcliSimpleException("Timed out waiting for FoD remediations FPR download to complete after "
                    + MAX_RETRIES + " retries");
        }
        return buildResultNode(releaseDescriptor, destination);
    }

    private Path getDestination(FoDReleaseDescriptor releaseDescriptor) {
        return outputFile == null ? Path.of(String.format("remediations_release_%s.fpr", releaseDescriptor.getReleaseId())) : outputFile.toPath();
    }

    private GetRequest getDownloadRequest(UnirestInstance unirest, FoDReleaseDescriptor releaseDescriptor, FoDScanDescriptor scanDescriptor) {
        return unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .headerReplace(HttpHeader.ACCEPT, "application/octet-stream")
                .queryString("scanType", scanDescriptor.getScanType());
    }

    private ObjectNode buildResultNode(FoDReleaseDescriptor releaseDescriptor, Path destination) {
        ObjectNode result = JsonHelper.getObjectMapper().createObjectNode();
        result.put("releasesDownloaded", 1);
        result.putArray("releaseIds").add(releaseDescriptor.getReleaseId());
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