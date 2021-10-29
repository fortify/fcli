package com.fortify.cli.dast.command.crud.scdast.scanstatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.transform.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transform.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.crud.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.crud.scdast.scanstatus.actions.SCDastScanStatusActionsHandler;
import com.fortify.cli.dast.command.crud.scdast.scanstatus.options.SCDastGetScanStatusOptions;
import com.fortify.cli.ssc.command.crud.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.Command;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Mixin;

public class SCDastScanStatusCommands {
    private static final String NAME = "scan-status";
    private static final String DESC = "DAST scan status";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {

        @ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanStatusOptions scanStatusOptions;

        @Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanStatusActionsHandler actionsHandler = new SCDastScanStatusActionsHandler(unirest);

            JsonNode response = actionsHandler.getScanStatus(scanStatusOptions.getScanId());

            outputOptionsHandler.printToFormat(response);

            return null;
        }

        @Override
        public IJsonNodeTransformer getJsonNodeTransformer(FieldBasedTransformerFactory fieldBasedTransformerFactory, OutputFormat format) {
            return new SSCApplicationCommands.TransformerSupplier().getJsonNodeTransformer(fieldBasedTransformerFactory, format);
        }
    }
}

