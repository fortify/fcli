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
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.Uninstall.CMD_NAME)
public class SSCPluginUninstallCommand extends AbstractSSCJsonNodeOutputCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private OutputHelperMixins.Uninstall outputHelper;
    @Mixin private SSCPluginResolverMixin.PositionalParameter pluginResolver;
    @Option(names="--no-auto-disable", negatable=true)
    private boolean autoDisable = true;
    
    @Override
    public JsonNode getJsonNode(UnirestInstance unirest) {
        String numericPluginId = pluginResolver.getNumericPluginId();
        // TODO Check whether plugin id exists
        JsonNode pluginData = disablePluginIfNecessary(unirest, numericPluginId);
        unirest.delete("/api/v1/plugins/{id}")
                    .routeParam("id", ""+numericPluginId)
                    .asObject(JsonNode.class).getBody();
        return pluginData;
    }
    
    @Override
    public String getActionCommandResult() {
        return "UNINSTALLED";
    }
    
    @Override
    public boolean isSingular() {
        return true;
    }
    
    private JsonNode disablePluginIfNecessary(UnirestInstance unirest, String numericPluginId) {
        JsonNode pluginData = unirest.get("/api/v1/plugins/{id}")
                .routeParam("id", ""+numericPluginId)
                .asObject(JsonNode.class).getBody();
        if ("STARTED".equals(pluginData.get("data").get("pluginState").asText()) ) {
            if ( !autoDisable ) {
                throw new IllegalStateException("Plugin cannot be deleted, as it is currently enabled, and --no-auto-disable has been specified");
            }
            SSCPluginStateHelper.disablePlugin(unirest, numericPluginId);
        }
        return pluginData;
    }
}
