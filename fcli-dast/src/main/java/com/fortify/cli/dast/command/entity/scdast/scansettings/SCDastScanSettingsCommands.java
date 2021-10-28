package com.fortify.cli.dast.command.entity.scdast.scansettings;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.command.util.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.command.util.output.OutputOptionsHandler;
import com.fortify.cli.common.json.transformer.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transformer.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scansettings.options.SCDastGetScanSettingsListOptions;
import com.fortify.cli.dast.command.entity.scdast.scansettings.options.SCDastGetScanSettingsOptions;
import com.fortify.cli.ssc.command.entity.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ArgGroup;

public class SCDastScanSettingsCommands {
    private static final String NAME = "scan-settings";
    private static final String DESC = "DAST scan settings";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {
        @ArgGroup(exclusive = false, heading = "Get a specific scan settings:%n", order = 1)
        @Getter private SCDastGetScanSettingsOptions scanSettingsOptions;

        @ArgGroup(exclusive = false, heading = "Filter multiple scan settings:%n", order = 2)
        @Getter private SCDastGetScanSettingsListOptions scanSettingsListOptions;


        @CommandLine.Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/application-version-scan-settings/scan-settings-summary-list";
            String urlParams = "";
            String dataNode = null;

            if (scanSettingsOptions != null){
                urlPath = "/api/v2/application-version-scan-settings/" + scanSettingsOptions.getScanSettingsId();
            } else {
                dataNode = "items";
                if(scanSettingsListOptions != null){
                    if (scanSettingsListOptions.getSearchText() != null){
                        urlParams += String.format("searchText=%s&",scanSettingsListOptions.getSearchText());
                    }
                    if(scanSettingsListOptions.getStartDate() != null){
                        urlParams += String.format("modifiedStartDate=%s&",scanSettingsListOptions.getStartDate());
                    }
                    if(scanSettingsListOptions.getEndDate() != null){
                        urlParams += String.format("modifiedEndDate=%s&",scanSettingsListOptions.getEndDate());
                    }
                    if(scanSettingsListOptions.getScanType() != null){
                        urlParams += String.format("scanType=%s&",scanSettingsListOptions.getScanType());
                    }
                }
            }

            JsonNode response = unirest.get(urlPath + "?" + urlParams)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody();

            if (dataNode != null){response = response.get(dataNode);}

            outputOptionsHandler.printToFormat(response);

            return null;
        }

        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }
}

