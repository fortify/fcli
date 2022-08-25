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
package com.fortify.cli.sc_dast.scan_settings.cli.cmd;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.IOutputConfigSupplier;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.output.cli.mixin.OutputMixin;
import com.fortify.cli.common.rest.runner.UnexpectedHttpResponseException;
import com.fortify.cli.sc_dast.rest.cli.cmd.AbstractSCDastUnirestRunnerCommand;
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
@Command(name = "list", description = "List scan settings on ScanCentral DAST")
public class SCDastScanSettingsListCommand extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {
        
        @ArgGroup(exclusive = false, headingKey = "arggroup.specific-scan-setting-options.heading", order = 1)
        private SCDastGetScanSettingsOptions scanSettingsOptions;

        @ArgGroup(exclusive = false, headingKey = "arggroup.list-scan-settings-options.heading", order = 2)
        private SCDastGetScanSettingsListOptions scanSettingsListOptions;


        @Mixin private OutputMixin outputMixin;

        @ReflectiveAccess
        public static class SCDastGetScanSettingsOptions {
            @Option(names = {"-i", "--id", "--scan-settings-id"})
            @Getter private String scanSettingsId;
        }
        
        @ReflectiveAccess
        public static class SCDastGetScanSettingsListOptions {
            @Option(names = {"-t","--text","--search-text"})
            @Getter private String searchText;

            @Option(names = {"--start","--start-date"})
            @Getter private String startDate;

            @Option(names = {"--end","--end-date"})
            @Getter private String endDate;

            private enum ScanTypes {Standard, WorkflowDriven, AMI}
            @Option(names = {"--type","--scan-type"})
            @Getter private ScanTypes scanType;
        }
        
        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/application-version-scan-settings/scan-settings-summary-list?" ;

            if (scanSettingsOptions != null){
                urlPath = "/api/v2/application-version-scan-settings/" + scanSettingsOptions.getScanSettingsId() + "?";
            } else {
                if(scanSettingsListOptions != null){
                    if (scanSettingsListOptions.getSearchText() != null){
                        urlPath += String.format("searchText=%s&",scanSettingsListOptions.getSearchText());
                    }
                    if(scanSettingsListOptions.getStartDate() != null){
                        urlPath += String.format("modifiedStartDate=%s&",scanSettingsListOptions.getStartDate());
                    }
                    if(scanSettingsListOptions.getEndDate() != null){
                        urlPath += String.format("modifiedEndDate=%s&",scanSettingsListOptions.getEndDate());
                    }
                    if(scanSettingsListOptions.getScanType() != null){
                        urlPath += String.format("scanType=%s&",scanSettingsListOptions.getScanType());
                    }
                }
            }

            try {
                outputMixin.write(unirest.get(urlPath)
                        .accept("application/json")
                        .header("Content-Type", "application/json"));
            } catch (UnexpectedHttpResponseException e) {
                ObjectNode output = new ObjectMapper().createObjectNode();
                String escapedPath = urlPath
                        .replace("/","\\/")
                        .replace("?","\\?");
                Pattern pattern = Pattern.compile("(?<="+escapedPath+": )(?<code>\\d{3})\\W(?<text>.*)");
                Matcher matcher = pattern.matcher(e.getMessage());
                String fields;
                if(matcher.find()){
                    output.put("statusCode", matcher.group("code"));
                    output.put("statusText", matcher.group("text"));
                    fields = "statusCode#statusText#message";
                } else {
                    output.put("statusText", "Error");
                    fields = "statusText#message";
                }
                output.put("message",e.getLocalizedMessage());
                outputMixin.overrideOutputFields(fields);

                outputMixin.write(output);
            }
            return null;
        }
        
        @Override
        public OutputConfig getOutputOptionsWriterConfig() {
            return SCDastOutputHelper.defaultTableOutputConfig().defaultColumns("id#name#cicdToken:Settings Id#");
        }
}
