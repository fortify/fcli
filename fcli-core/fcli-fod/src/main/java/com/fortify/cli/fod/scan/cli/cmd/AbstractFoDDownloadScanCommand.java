/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.output.mixin.FoDOutputHelperMixins;
import com.fortify.cli.fod.release.cli.mixin.FoDReleaseByQualifiedNameOrIdResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanType;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

@Command(name = FoDOutputHelperMixins.DownloadResults.CMD_NAME)
public abstract class AbstractFoDDownloadScanCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDReleaseByQualifiedNameOrIdResolverMixin.RequiredOption releaseResolver;

    @Option(names = {"-f", "--fpr"}, required = true)
    private File outputFile;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var releaseDescriptor = releaseResolver.getReleaseDescriptor(unirest);

        FoDScanDescriptor lastScan = FoDScanHelper.getLatestScanDescriptor(unirest, releaseDescriptor.getReleaseId(),
                FoDScanType.valueOf(getDownloadScanType()), false);
        FoDScanHelper.validateScanDate(lastScan, FoDScanHelper.MAX_RETENTION_PERIOD);

        int status = 202; int maxRetries = 10;
        while ( status==202 ) {
            status = unirest.get("/api/v3/releases/{releaseId}/fpr")
                .routeParam("releaseId", releaseDescriptor.getReleaseId())
                .accept("application/octet-stream")
                .queryString("scanType", getDownloadScanType())
                .asFile(outputFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
                .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
            //if ( maxRetries++ >= 5) { throw new RuntimeException("Timed out while trying to retrieve latest scan results."); }
        }
        return releaseDescriptor.asObjectNode()
                .put("scanType", getDownloadScanType())
                .put("file", outputFile.getName());
    }

    protected abstract String getDownloadScanType();

    @Override
    public String getActionCommandResult() {
        return "FPR_DOWNLOADED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
