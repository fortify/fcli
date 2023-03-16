/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.plugin.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.cmd.unirest.IUnirestJsonNodeSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.ssc.output.cli.cmd.AbstractSSCOutputCommand;
import com.fortify.cli.ssc.output.cli.mixin.SSCOutputHelperMixins;
import com.fortify.cli.ssc.plugin.cli.mixin.SSCPluginResolverMixin;
import com.fortify.cli.ssc.plugin.helper.SSCPluginStateHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SSCOutputHelperMixins.Uninstall.CMD_NAME)
public class SSCPluginUninstallCommand extends AbstractSSCOutputCommand implements IUnirestJsonNodeSupplier, IActionCommandResultSupplier {
    @Getter @Mixin private SSCOutputHelperMixins.Uninstall outputHelper;
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
