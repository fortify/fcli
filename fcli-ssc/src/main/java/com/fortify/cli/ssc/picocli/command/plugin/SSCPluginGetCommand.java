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

import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.common.output.cli.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.OutputConfig;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@ReflectiveAccess
@Command(name = "get")
public class SSCPluginGetCommand extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
	@CommandLine.Mixin private OutputMixin outputMixin;

	@CommandLine.Mixin
	private SSCPluginCommonOptions.SSCPluginSelectSingleRequiredMixin id;

	@SneakyThrows
	protected Void runWithUnirest(UnirestInstance unirest) {
		outputMixin.write(
				unirest.get(SSCUrls.PLUGIN(id.getNumericPluginId(unirest).toString()))
		);
		return null;
	}

	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SSCOutputHelper.defaultTableOutputConfig()
				.defaultColumns("id#pluginId#pluginType#pluginName#pluginVersion#pluginState");
	}
}
