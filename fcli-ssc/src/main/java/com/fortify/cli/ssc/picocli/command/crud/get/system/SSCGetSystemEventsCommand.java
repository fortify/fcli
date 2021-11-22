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
package com.fortify.cli.ssc.picocli.command.crud.get.system;

import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.ssc.picocli.command.AbstractSSCUnirestRunnerCommand;
import com.fortify.cli.ssc.picocli.command.crud.get.SSCGetCommand;
import com.fortify.cli.ssc.picocli.constants.system.SSCSystemEventConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SSCGetSystemEventsCommand extends SSCSystemEventConstants.Plural {
	@ReflectiveAccess
	@SubcommandOf(SSCGetSystemCommand.Impl.class)
	@Command(name = CMD, description = DESC_GET /*, aliases = {ALIAS}*/)
	@RequiresProduct(ProductOrGroup.SSC)
	public static final class Impl extends AbstractSSCUnirestRunnerCommand implements IOutputConfigSupplier {
		@CommandLine.Mixin private OutputMixin outputMixin;
		
		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			outputMixin.write(
				unirest.get("/api/v1/events?limit=-1")
					.accept("application/json")
					.header("Content-Type", "application/json")
			);
			
			return null;
		}
		
		@Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SSCGetCommand.defaultOutputConfig().defaultColumns(OUTPUT_COLUMNS);
		}
	}
}
