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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.sc_sast._common.output.cli.cmd.AbstractSCSastControllerJsonNodeOutputCommand;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Status.CMD_NAME)
public class SCSastControllerScanStatusCommand extends AbstractSCSastControllerJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Status outputHelper;
    @Mixin SCSastScanJobResolverMixin.PositionalParameter scanJobResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return scanJobResolver.getScanJobDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
