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
package com.fortify.cli.ssc.app.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.ssc._common.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;
import com.fortify.cli.ssc.app.cli.mixin.SSCAppResolverMixin;

import kong.unirest.core.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Get.CMD_NAME)
public class SSCAppGetCommand extends AbstractSSCJsonNodeOutputCommand {
    @Getter @Mixin private OutputHelperMixins.Get outputHelper; 
    @Mixin private SSCAppResolverMixin.PositionalParameter appResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return appResolver.getAppDescriptor(unirest).asJsonNode();
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
