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
package com.fortify.cli.sc_dast.picocli.command.crud.get.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.sc_dast.picocli.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.picocli.command.constants.scan.SCDastScanResultsConstants;
import com.fortify.cli.sc_dast.picocli.command.crud.get.SCDastGetCommand;
import com.fortify.cli.sc_dast.picocli.command.util.SCDastScanResultsActionsHandler;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class SCDastGetScanResultsCommand extends SCDastScanResultsConstants.Plural {
	@ReflectiveAccess
	@SubcommandOf(SCDastGetScanCommand.Impl.class)
	@Command(name = CMD, description = DESC_GET /* , aliases = {ALIAS} */)
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static final class Impl extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
		@ArgGroup(exclusive = false, heading = "Get results from a specific scan:%n", order = 1)
		private SCDastScanResultsOptions scanResultsOptions;

		@Mixin private OutputMixin outputMixin;

		@ReflectiveAccess
		public static class SCDastScanResultsOptions {
			@Option(names = { "-i", "--id", "--scan-id" }, description = "The scan id", required = true)
			@Getter private int scanId;

			@Option(names = { "-w", "--wait", "--wait-completion" }, defaultValue = "false", description = "Wait while the scan is Queued, Pending, or Running. Then displays scan results. ")
			@Getter private boolean waitCompletion;

			@Option(names = { "--interval", "--wait-interval" }, defaultValue = "30", description = "When waiting for completion, how long between to poll, in seconds", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
			@Getter private int waitInterval;

			@Option(names = { "--detailed" }, defaultValue = "false", description = "Displays issues count while polling scans status", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
			@Getter private boolean detailed;
		}

		@SneakyThrows
		protected Void runWithUnirest(UnirestInstance unirest) {
			SCDastScanResultsActionsHandler actionsHandler = new SCDastScanResultsActionsHandler(unirest);

			if (scanResultsOptions.isWaitCompletion()) {
				if (scanResultsOptions.isDetailed()) {
					actionsHandler.waitCompletionWithDetails(scanResultsOptions.getScanId(),
							scanResultsOptions.getWaitInterval());
				} else {
					actionsHandler.waitCompletion(scanResultsOptions.getScanId(), scanResultsOptions.getWaitInterval());
				}
			}

			JsonNode response = actionsHandler.getScanResults(scanResultsOptions.getScanId());

			outputMixin.write(response);

			return null;
		}

		@Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig().defaultColumns(OUTPUT_COLUMNS);
		}
	}
}
