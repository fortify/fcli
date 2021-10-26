
package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.output.OutputWriterMixin;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.scdast.scansettings.SCDastScanSettingsCommands;
import com.fortify.cli.dast.command.entity.scdast.scansettings.SCDastScanSettingsOptions;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanListCommands {
    private static final String NAME = "list";
    private static final String DESC = "DAST scan";

    @ReflectiveAccess
    @SubcommandOf(SCDastScanCommands.Get.class)
    @Command(name = NAME, description = "Get list of " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Filter scan settings:%n", order = 1)
        @Getter private SCDastScanListOptions scanListOptions;

        @Mixin
        private OutputWriterMixin outputWriterMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/scan-summary-list";
            String urlParams = "";

            if(scanListOptions != null){
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
