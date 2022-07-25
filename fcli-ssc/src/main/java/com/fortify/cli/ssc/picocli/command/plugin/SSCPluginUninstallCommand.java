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
package com.fortify.cli.ssc.picocli.command.plugin;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.picocli.command.plugin.SSCPluginCommonOptions.SSCPluginSelectSingleRequiredOptions;
import com.fortify.cli.ssc.util.SSCOutputHelper;

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
	protected Void runWithUnirest(UnirestInstance unirest) {
		String numericPluginId = ""+deleteOptions.getNumericPluginId(unirest);
		// TODO Check whether plugin id exists
		disablePluginIfNecessary(unirest, numericPluginId);
		outputMixin.write(
				unirest.delete("/api/v1/plugins/{id}")
					.routeParam("id", ""+numericPluginId));
		return null;
	}
	
	private void disablePluginIfNecessary(UnirestInstance unirest, String numericPluginId) {
		JsonNode pluginData = unirest.get("/api/v1/plugins/{id}?fields=pluginState")
				.routeParam("id", ""+numericPluginId)
				.asObject(JsonNode.class).getBody();
		if ("STARTED".equals(pluginData.get("data").get("pluginState").asText()) ) {
			if ( !deleteOptions.autoDisable ) {
				throw new IllegalStateException("Plugin cannot be deleted, as it is currently enabled, and --no-auto-disable has been specified");
			}
			unirest.post("/api/v1/plugins/action/disable")
				.body(new PluginIds(numericPluginId))
				.asEmpty();
		}
	}
	
	private static final class PluginIds {
		public PluginIds(String... pluginIds) {
			this.pluginIds = Arrays.stream(pluginIds).mapToInt(Integer::parseInt).toArray();
		}
		@JsonProperty private int[] pluginIds;
	}

	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("message#errorCode#responseCode");
	}
}
