package com.fortify.cli.sc_dast.command.crud.scanstatus;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands.SCDastGetCommand;
import com.fortify.cli.sc_dast.command.crud.scanstatus.actions.SCDastScanStatusActionsHandler;
import com.fortify.cli.sc_dast.command.crud.scanstatus.options.SCDastGetScanStatusOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanStatusCommands {
    private static final String NAME = "scan-status";
    private static final String DESC = "DAST scan status";

    @ReflectiveAccess
    @SubcommandOf(SCDastCrudRootCommands.SCDastGetCommand.class)
    @Command(name = NAME, description = "Get " + DESC + " from SC DAST")
    public static final class Get extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {

        @ArgGroup(exclusive = false, heading = "Get a specific scan:%n", order = 1)
        @Getter private SCDastGetScanStatusOptions scanStatusOptions;

        @Mixin
        @Getter private OutputMixin outputMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanStatusActionsHandler actionsHandler = new SCDastScanStatusActionsHandler(unirest);
            outputMixin.write(actionsHandler.getScanStatus(scanStatusOptions.getScanId()));
            return null;
        }
        
        @Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig(); // TODO .defaultColumns(_getDefaultOutputColumns());
		}
    }
}

