
package com.fortify.cli.dast.command.entity.scdast.scansettings.list;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;

import com.fortify.cli.dast.command.entity.scdast.scansettings.SCDastScanSettingsCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanSettingsListCommands {
    private static final String NAME = "list";
    private static final String DESC = "DAST scan settings";

    @ReflectiveAccess
    @SubcommandOf(SCDastScanSettingsCommands.Get.class)
    @Command(name = NAME, description = "Get list of " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Filter scan settings:%n", order = 1)
        @Getter private SCDastScanSettingsListOptions scanSettingsOptions;

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

            outputWriterMixin.printToFormat(response);

            return null;
        }

    }


}
