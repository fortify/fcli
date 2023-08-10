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
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Cancel.CMD_NAME)
public class FoDScanCancelCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Cancel outputHelper;

    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;


    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        FoDScanDescriptor descriptor = scanResolver.getScanDescriptor(unirest);
        unirest.post(FoDUrls.RELEASE + "/scans/{scanId}/cancel-scan")
                .routeParam("relId", String.valueOf(descriptor.getReleaseId()))
                .routeParam("scanId", String.valueOf(descriptor.getScanId()));
        return descriptor.asJsonNode();
    }

    public JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public String getActionCommandResult() {
        return "CANCELLED";
    }

    @Override
    public boolean isSingular() {
        return true;
    }
}
