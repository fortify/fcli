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
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.ssc.plugin.cli.cmd.SSCPluginCommonOptions.SSCPluginSelectSingleRequiredOptions;
import com.fortify.cli.ssc.plugin.helper.SSCPluginStateHelper;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "uninstall", aliases = {"rm"})
public class SSCPluginUninstallCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
    @Mixin private OutputMixin outputMixin;
    @ArgGroup(headingKey = "fcli.ssc.plugin.uninstall.options.heading", exclusive = false)
    private SSCPluginDeleteOptions deleteOptions;
    
    private static class SSCPluginDeleteOptions {
        @ArgGroup(exclusive=false) SSCPluginSelectSingleRequiredOptions pluginSelectOptions;
        @Option(names="--no-auto-disable", negatable=true)
        private boolean autoDisable = true;
        
        public Integer getNumericPluginId(UnirestInstance unirest) {
            return pluginSelectOptions==null ? null : pluginSelectOptions.getNumericPluginId();
        }
    }
    
    @SneakyThrows
    protected Void run(UnirestInstance unirest) {
        int numericPluginId = deleteOptions.getNumericPluginId(unirest);
        // TODO Check whether plugin id exists
        disablePluginIfNecessary(unirest, numericPluginId);
        outputMixin.write(
                unirest.delete("/api/v1/plugins/{id}")
                    .routeParam("id", ""+numericPluginId));
        return null;
    }
    
    private void disablePluginIfNecessary(UnirestInstance unirest, int numericPluginId) {
        JsonNode pluginData = unirest.get("/api/v1/plugins/{id}?fields=pluginState")
                .routeParam("id", ""+numericPluginId)
                .asObject(JsonNode.class).getBody();
        if ("STARTED".equals(pluginData.get("data").get("pluginState").asText()) ) {
            if ( !deleteOptions.autoDisable ) {
                throw new IllegalStateException("Plugin cannot be deleted, as it is currently enabled, and --no-auto-disable has been specified");
            }
            SSCPluginStateHelper.disablePlugin(unirest, numericPluginId);
        }
    }

    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.table()
                .defaultColumns("message#errorCode#responseCode");
    }
}
