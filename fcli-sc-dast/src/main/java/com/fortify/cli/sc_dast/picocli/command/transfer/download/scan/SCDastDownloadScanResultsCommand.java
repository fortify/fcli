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
package com.fortify.cli.sc_dast.picocli.command.transfer.download.scan;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.config.product.ProductOrGroup;
import com.fortify.cli.common.picocli.annotation.RequiresProduct;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.sc_dast.picocli.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.picocli.command.crud.get.SCDastGetCommand;
import com.fortify.cli.sc_dast.picocli.constants.scan.SCDastScanResultsConstants;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class SCDastDownloadScanResultsCommand extends SCDastScanResultsConstants.Plural {
	@ReflectiveAccess
	@SubcommandOf(SCDastDownloadScanCommand.Impl.class)
	@Command(name = CMD, description = DESC_DOWNLOAD /* , aliases = {ALIAS} */)
	@RequiresProduct(ProductOrGroup.SC_DAST)
	public static final class Impl extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
		@ArgGroup(exclusive = false, heading = "Download results from a specific scan:%n", order = 1)
        private SCDastTransferScanResultsOptions scanResultsOptions;

        @Mixin
        private OutputMixin OutputMixin;
        
		@ReflectiveAccess
		public class SCDastTransferScanResultsOptions {

		    @Option(names = {"-i","--id", "--scan-id"}, description = "The scan id", required = true)
		    @Getter private int scanId;

		    @Option(names = {"-f", "--file", "--output-file"}, description = "The output file to save the scan results in.", required = true)
		    @Getter private String file;
		}
		
		@SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest){
			File outputFile = unirest.get("/api/v2/scans/{scanId}/download-results")
		    		.routeParam("scanId", String.valueOf(scanResultsOptions.getScanId()))
		            .accept("application/json")
		            .header("Content-Type", "application/json")
		            .asFile(scanResultsOptions.getFile())
		            .getBody(); // TODO Do we need to call getBody()? Do we need to do anything with the return value? 

			    ObjectNode output = new ObjectMapper().createObjectNode();
			    output.put("path", outputFile.getPath());

	            OutputMixin.write(output);
	            return null;
        }

		@Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig().defaultColumns("path");
		}
	}
}
