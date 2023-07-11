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
package com.fortify.cli.ssc.entity.plugin.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.entity.plugin.cli.mixin.SSCPluginResolverMixin;
import com.fortify.cli.ssc.entity.plugin.helper.SSCPluginStateHelper;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCJsonNodeOutputCommand;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = OutputHelperMixins.Disable.CMD_NAME)
// TODO Check whether plugin exists, and isn't disabled already
public class SSCPluginDisableCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Disable outputHelper;
    @Mixin private SSCPluginResolverMixin.PositionalParameter pluginResolver;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        return SSCPluginStateHelper.disablePlugin(unirest, pluginResolver.getNumericPluginId());
    }

    @Override
    public String getActionCommandResult() {
        return "DISABLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
}
