package com.fortify.cli.dast.command.entity.scdast.scanresults;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.transform.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transform.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scanresults.actions.SCDastScanResultsActionsHandler;
import com.fortify.cli.dast.command.entity.scdast.scanresults.options.SCDastScanResultsOptions;
import com.fortify.cli.ssc.command.crud.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Command;

public class SCDastScanResultsCommands {
    private static final String NAME = "scan-results";
    private static final String DESC = "DAST scan results";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {

        @ArgGroup(exclusive = false, heading = "Get results from a specific scan:%n", order = 1)
        @Getter private SCDastScanResultsOptions scanResultsOptions;

        @Mixin
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

