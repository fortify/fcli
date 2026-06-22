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
package com.fortify.cli.fod._common.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.fod._common.cli.mixin.FoDDelimiterMixin;
import com.fortify.cli.fod._common.output.cli.cmd.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod._common.scan.helper.FoDScanHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanPutRequest;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;
import com.fortify.cli.fod._common.util.FoDEnums;
import com.fortify.cli.fod.attribute.cli.mixin.FoDAttributeUpdateOptions;
import com.fortify.cli.fod.attribute.helper.FoDAttributeDefinitionHelper;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan")
public abstract class AbstractFoDScanUpdateCommand extends AbstractFoDJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Mixin private FoDDelimiterMixin delimiterMixin;
    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;
    @Mixin private FoDAttributeUpdateOptions.RequiredAttrOption scanAttrsUpdate;

    @Override
    public final JsonNode getJsonNode(UnirestInstance unirest) {
        FoDScanDescriptor descriptor = scanResolver.getScanDescriptor(unirest, getScanType());
        JsonNode jsonAttrs = new FoDAttributeDefinitionHelper(unirest).buildAttributesNodeForUpdate(
                FoDEnums.AttributeTypes.Scan,
                descriptor.getAttributes(),
                scanAttrsUpdate.getAttributes(),
                false);
        FoDScanPutRequest request = FoDScanPutRequest.builder().attributes(jsonAttrs).build();
        return FoDScanHelper.updateScan(unirest, descriptor.getScanId(), request).asJsonNode();
    }

    protected abstract FoDScanType getScanType();

    @Override
    public final String getActionCommandResult() {
        return "UPDATED";
    }

    @Override
    public final boolean isSingular() {
        return true;
    }
}
