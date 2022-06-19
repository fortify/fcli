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
package com.fortify.cli.sc_dast.picocli.command.scan;

import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.sc_dast.picocli.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.util.SCDastOutputHelper;
import com.fortify.cli.sc_dast.util.SCDastScanStatusTypes;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@ReflectiveAccess
@Command(name = "list", description = "List DAST scans on ScanCentral DAST")
public class SCDastScanListCommand extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
    @ArgGroup(exclusive = false, heading = "arggroup.get-specific-scan-options.heading", order = 1)
    private SCDastGetScanOptions scanOptions;

    @ArgGroup(exclusive = false, headingKey = "arggroup.filter-multiple-scans-options.heading", order = 2)
    private SCDastGetScanListOptions scanListOptions;
    
    @ReflectiveAccess
    public static final class SCDastGetScanListOptions {
        @Option(names = {"-t", "--text", "--search-text"})
        @Getter private String searchText;

        @Option(names = { "--start","--start-date"})
        @Getter private String startDate;

        @Option(names = {"--end","--end-date"})
        @Getter private String endDate;

        @Option(names = {"-s","--status", "--scan-status"})
        @Getter private SCDastScanStatusTypes scanStatus;

        @Option(names = {"--order","--order-by"})
        @Getter private String orderBy;

        private enum Directions {ASC, DESC}
        @Option(names = {"-d","--direction","--order-by-direction"})
        @Getter private Directions orderByDirection;
    }
    
    @ReflectiveAccess
    public static class SCDastGetScanOptions {
        @Option(names = {"-i", "--id", "--scan-id"})
        @Getter private String scanId;
    }

    @Mixin private OutputMixin outputMixin;

    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        String urlPath = "/api/v2/scans/scan-summary-list";
        String urlParams = "";

        if (scanOptions != null){
            urlPath = "/api/v2/scans/"+ scanOptions.getScanId() + "/scan-summary";
        }
        else {
            if(scanListOptions != null) {
                if (scanListOptions.getSearchText() != null) {
                    urlParams += String.format("searchText=%s&",scanListOptions.getSearchText());
                }
                if(scanListOptions.getStartDate() != null) {
                    urlParams += String.format("startedOnStartDate=%s&",scanListOptions.getStartDate());
                }
                if(scanListOptions.getEndDate() != null) {
                    urlParams += String.format("startedOnEndDate=%s&",scanListOptions.getEndDate());
                }
                if(scanListOptions.getOrderBy() != null) {
                    urlParams += String.format("orderBy=%s&",scanListOptions.getOrderBy());
                }
                if(scanListOptions.getOrderByDirection() != null) {
                    urlParams += String.format("orderByDirection=%s&",scanListOptions.getOrderByDirection());
                }
                if(scanListOptions.getScanStatus() != null) {
                    urlParams += String.format("scanStatusType=%s&",scanListOptions.getScanStatus());
                }
            }
        }

        outputMixin.write( unirest.get(urlPath + "?" + urlParams)
                .accept("application/json")
                .header("Content-Type", "application/json"));

        return null;
    }
	
	@Override
	public OutputConfig getOutputOptionsWriterConfig() {
		return SCDastOutputHelper.defaultTableOutputConfig().defaultColumns("id#" +
                "name#" +
                "applicationName:Application#" +
                "applicationVersionName:Version#" +
                "scanStatusType:Status#"+
                "lowCount:Low#" +
                "mediumCount:Medium#" +
                "highCount:High#" +
                "criticalCount:Critical");
	}
}
