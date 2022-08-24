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
package com.fortify.cli.sc_dast.scan_output.cli;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.OutputConfig;
import com.fortify.cli.common.output.cli.OutputMixin;
import com.fortify.cli.sc_dast.rest.cli.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.util.SCDastOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "download-logs")
public class SCDastScanOutputDownloadLogsCommand extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
		@ArgGroup(exclusive = false, headingKey = "arggroup.download-logs-options.heading", order = 1)
        @Getter private SCDastTransferScanLogsOptions scanLogsOptions;

        @Mixin
        @Getter private OutputMixin OutputMixin;
		
		@ReflectiveAccess
		public static class SCDastTransferScanLogsOptions {
		    @Option(names = {"-i","--id", "--scan-id"}, required = true)
		    @Getter private int scanId;

		    @Option(names = {"-f", "--file", "--output-file"}, required = true)
		    @Getter private String file;
		}
		
		@SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest){
            File outputFile = unirest.get("/api/v2/scans/{scanId}/download-logs")
	    		.routeParam("scanId", String.valueOf(scanLogsOptions.getScanId()))
	            .accept("application/json")
	            .header("Content-Type", "application/json")
	            .asFile(scanLogsOptions.getFile())
	            .getBody(); // TODO Do we need to call getBody()? Do we need to do anything with the return value? 

		    ObjectNode output = new ObjectMapper().createObjectNode();
		    output.put("path", outputFile.getPath());

            OutputMixin.write(output);
            return null;
        }

		@Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastOutputHelper.defaultTableOutputConfig().defaultColumns("path"); // TODO Move to constants?
		}
}
