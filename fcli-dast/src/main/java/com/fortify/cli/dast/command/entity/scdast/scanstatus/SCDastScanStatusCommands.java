package com.fortify.cli.dast.command.entity.scdast.scanstatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.command.util.annotation.SubcommandOf;
import com.fortify.cli.common.command.util.output.IJsonNodeTransformerSupplier;
import com.fortify.cli.common.command.util.output.OutputOptionsHandler;
import com.fortify.cli.common.json.transformer.FieldBasedTransformerFactory;
import com.fortify.cli.common.json.transformer.IJsonNodeTransformer;
import com.fortify.cli.common.output.OutputFormat;
import com.fortify.cli.common.util.JsonNodeFilterHandler;
import com.fortify.cli.dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.dast.command.entity.SCDastEntityRootCommands;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.actions.SCDastScanStatusActionsHandler;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.options.SCDastGetScanStatusOptions;
import com.fortify.cli.dast.command.entity.types.ScanStatusTypes;
import com.fortify.cli.ssc.command.entity.SSCApplicationCommands;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.Set;

public class SCDastScanStatusCommands {
    private static final String NAME = "scan-status";
    private static final String DESC = "DAST scan status";

    @ReflectiveAccess
    @SubcommandOf(SCDastEntityRootCommands.SCDASTGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IJsonNodeTransformerSupplier {

        @CommandLine.ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanStatusOptions scanStatusOptions;

        @CommandLine.Mixin
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

