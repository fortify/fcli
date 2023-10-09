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
package com.fortify.cli.fod.oss_scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanResolverMixin;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.StandardCopyOption;

@Command(name = OutputHelperMixins.Download.CMD_NAME)
public class FoDOSSScanDownloadCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Download outputHelper;
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;

    @Option(names = {"-f", "--sbom"}, required = true)
    private File outputFile;

    @Override @SneakyThrows
    public JsonNode getJsonNode(UnirestInstance unirest) {
        var scanDescriptor = scanResolver.getScanDescriptor(unirest);
        int status = 202;
        while ( status==202 ) {
            status = unirest.get("/api/v3/open-source-scans/{scanId}/sbom")
                .routeParam("scanId", String.valueOf(scanDescriptor.getScanId()))
                .accept("application/octet-stream")
                .asFile(outputFile.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING)
                .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return scanDescriptor.asObjectNode()
                .put("scanType", "OpenSource")
                .put("file", outputFile.getName());
    }

    @Override
    public String getActionCommandResult() {
        return "SBOM_DOWNLOADED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
