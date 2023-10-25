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
import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.fod._common.output.cli.AbstractFoDJsonNodeOutputCommand;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.UnirestInstance;
import picocli.CommandLine.Mixin;

@CommandGroup("*-scan")
public abstract class AbstractFoDScanGetCommand extends AbstractFoDJsonNodeOutputCommand {
    @Mixin private FoDScanResolverMixin.PositionalParameter scanResolver;

    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return scanResolver.getScanDescriptor(unirest, getScanType()).asJsonNode();
    }
    
    protected abstract FoDScanType getScanType();

    @Override
    public boolean isSingular() {
        return true;
    }
}
