/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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
package com.fortify.cli.dast.command.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.entity.scdast.SCDastScanSettingsOptions;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputFilterOptions;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanSettingsCommands {
    private static final String NAME = "scan-settings";
    private static final String DESC = "DAST scan settings";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Filter scan settings:%n", order = 10)
        @Getter private SCDastScanSettingsOptions scanSettingsOptions;

        @ArgGroup(exclusive = false, heading = "Output filter options :%n", order = 20)
        @Getter private OutputFilterOptions outputFilterOptions;

        @Mixin
        private OutputWriterMixin outputWriterMixin;


        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/application-version-scan-settings/scan-settings-summary-list";
            String urlParams = "";

            if(scanSettingsOptions != null){
                if (scanSettingsOptions.getSearchText() != null){
                    urlParams += String.format("searchText=%s&",scanSettingsOptions.getSearchText());
                }
                if(scanSettingsOptions.getStartDate() != null){
                    urlParams += String.format("modifiedStartDate=%s&",scanSettingsOptions.getStartDate());
                }
                if(scanSettingsOptions.getEndDate() != null){
                    urlParams += String.format("modifiedEndDate=%s&",scanSettingsOptions.getEndDate());
                }
                if(scanSettingsOptions.getScanType() != null){
                    urlParams += String.format("scanType=%s&",scanSettingsOptions.getScanType());
                }
            }

            JsonNode response = unirest.get(urlPath + "?" + urlParams)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get("items");

            response = outputFilterOptions.filterOutput(response);
            outputWriterMixin.printToFormat(response);

            return null;
        }

    }


}
