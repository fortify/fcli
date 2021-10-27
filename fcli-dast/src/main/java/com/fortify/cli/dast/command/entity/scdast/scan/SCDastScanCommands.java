package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.command.util.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.command.util.output.OutputWriterMixin;
import com.fortify.cli.common.json.transformer.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transformer.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFilterOptions;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scan.options.SCDastGetScanListOptions;
import com.fortify.cli.dast.command.entity.scdast.scan.options.SCDastGetScanOptions;
import com.fortify.cli.dast.command.entity.scdast.scan.options.SCDastScanOptions;
import com.fortify.cli.ssc.command.entity.SSCApplicationCommands;
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
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier  {

        @ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanOptions scanOptions;


        @ArgGroup(exclusive = false, heading = "Filter multiple scans:%n", order = 2)
        @Getter private SCDastGetScanListOptions scanListOptions;

        @Mixin
        private OutputWriterMixin outputWriterMixin;

        @ArgGroup(exclusive = false, heading = "Filter Output:%n", order = 10)
        @Getter private OutputFilterOptions outputFilterOptions;

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

            if (outputFilterOptions != null ){
                response = outputFilterOptions.filterOutput(response);
            }
            outputWriterMixin.printToFormat(response);

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
        private OutputWriterMixin outputWriterMixin;

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

            outputWriterMixin.printToFormat(response);

            return null;
        }

        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }
}
