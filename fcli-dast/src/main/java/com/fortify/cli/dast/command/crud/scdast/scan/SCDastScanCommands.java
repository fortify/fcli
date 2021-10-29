package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.transform.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transform.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.crud.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.crud.scdast.scan.options.SCDastGetScanListOptions;
import com.fortify.cli.dast.command.crud.scdast.scan.options.SCDastGetScanOptions;
import com.fortify.cli.dast.command.crud.scdast.scan.options.SCDastScanOptions;
import com.fortify.cli.ssc.command.crud.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;
import picocli.CommandLine.ArgGroup;


public class SCDastScanCommands {
    private static final String NAME = "scan";
    private static final String DESC = "DAST scan";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {

        @ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanOptions scanOptions;


        @ArgGroup(exclusive = false, heading = "Filter multiple scans:%n", order = 2)
        @Getter private SCDastGetScanListOptions scanListOptions;

        @Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/scan-summary-list";
            String urlParams = "";
            String dataNode;

            if (scanOptions != null){
                urlPath = "/api/v2/scans/"+ scanOptions.getScanId() + "/scan-summary";
                dataNode = "item";
            }
            else {
                dataNode = "items";
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

            JsonNode response = unirest.get(urlPath + "?" + urlParams)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .asObject(ObjectNode.class)
                    .getBody()
                    .get(dataNode);

            outputOptionsHandler.printToFormat(response);

            return null;
        }


        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTScanCommand.class)
    @Command(name = NAME, description = "Start " + DESC + " using SC DAST")
    public static final class Scan extends AbstractSCDastUnirestRunnerCommand  implements IJsonNodeTransformerSupplier {

        @ArgGroup(exclusive = false, heading = "Scan options:%n", order = 1)
        @Getter
        private SCDastScanOptions scanOptions;

        @Mixin
        private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            String urlPath = "/api/v2/scans/start-scan-cicd";
            String body = scanOptions.getJsonBody();

            JsonNode response = unirest.post(urlPath)
                    .accept("application/json")
                    .header("Content-Type", "application/json")
                    .body(body)
                    .asObject(ObjectNode.class)
                    .getBody();

            outputOptionsHandler.printToFormat(response);

            return null;
        }


        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }
}
