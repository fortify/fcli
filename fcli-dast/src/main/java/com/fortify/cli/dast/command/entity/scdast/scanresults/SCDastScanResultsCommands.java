package com.fortify.cli.dast.command.entity.scdast.scanresults;

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
import com.fortify.cli.dast.command.entity.scdast.scanresults.actions.SCDastScanResultsActionsHandler;
import com.fortify.cli.dast.command.entity.scdast.scanresults.options.SCDastScanResultsOptions;
import com.fortify.cli.ssc.command.entity.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

public class SCDastScanResultsCommands {
    private static final String NAME = "scan-results";
    private static final String DESC = "DAST scan results";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {

        @CommandLine.ArgGroup(exclusive = false, heading = "Get results from a specific scan:%n", order = 1)
        @Getter private SCDastScanResultsOptions scanResultsOptions;

        @CommandLine.Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanResultsActionsHandler actionsHandler = new SCDastScanResultsActionsHandler(unirest);

            if(scanResultsOptions.isWaitCompletion()) {
                actionsHandler.waitCompletion(scanResultsOptions.getScanId(), scanResultsOptions.getWaitInterval());
            }

            JsonNode response = actionsHandler.getScanResults(scanResultsOptions.getScanId());

            outputOptionsHandler.printToFormat(response);

            return null;
        }


        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }
}

