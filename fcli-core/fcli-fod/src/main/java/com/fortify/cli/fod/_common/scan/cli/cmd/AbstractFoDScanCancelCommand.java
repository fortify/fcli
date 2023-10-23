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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.transform.IRecordTransformer;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

public abstract class AbstractFoDScanCancelCommand extends AbstractFoDJsonNodeOutputCommand implements IRecordTransformer, IActionCommandResultSupplier {
    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        FoDScanDescriptor descriptor = scanResolver.getScanDescriptor(unirest, getScanType());
        unirest.post(FoDUrls.RELEASE + "/scans/{scanId}/cancel-scan")
                .routeParam("relId", String.valueOf(descriptor.getReleaseId()))
                .routeParam("scanId", String.valueOf(descriptor.getScanId()));
        return descriptor.asJsonNode();
    }
    
    protected abstract FoDScanType getScanType();

    public final JsonNode transformRecord(JsonNode record) {
        return FoDScanHelper.renameFields(record);
    }

    @Override
    public final String getActionCommandResult() {
        return "CANCELLED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
