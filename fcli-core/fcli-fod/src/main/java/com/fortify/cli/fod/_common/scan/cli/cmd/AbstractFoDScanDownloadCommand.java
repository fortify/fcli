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
package com.fortify.cli.fod._common.scan.cli.cmd;

import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan-download")
public abstract class AbstractFoDScanDownloadCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin; // Is automatically injected in resolver mixins
    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;
    @Mixin private CommonOptionMixins.RequiredFile outputFileMixin;

    @Override @SneakyThrows
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        FoDScanDescriptor scanDescriptor = scanResolver.getScanDescriptor(unirest, getScanType());
        FoDScanHelper.validateScanDate(scanDescriptor, FoDScanHelper.MAX_RETENTION_PERIOD);
        var file = outputFileMixin.getFile().getAbsolutePath();
        GetRequest request = getDownloadRequest(unirest, scanDescriptor);

        int status = 202;
        while ( status==202 ) {
            status = request
                .asFile(file, StandardCopyOption.REPLACE_EXISTING)
                .getStatus();
            if ( status==202 ) { Thread.sleep(30000L); }
        }
        return scanDescriptor.asObjectNode().put("file", file);
    }

    protected abstract GetRequest getDownloadRequest(UnirestInstance unirest, FoDScanDescriptor scanDescriptor);
    protected abstract FoDScanType getScanType();

    @Override
    public final String getActionCommandResult() {
        return "SCAN_DOWNLOADED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
