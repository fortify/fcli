package com.fortify.cli.sc_dast.command.transfer.scanlogs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.mixin.output.IOutputConfigSupplier;
import com.fortify.cli.common.picocli.mixin.output.OutputMixin;
import com.fortify.cli.common.picocli.mixin.output.OutputConfig;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands.SCDastGetCommand;
import com.fortify.cli.sc_dast.command.transfer.SCDastTransferRootCommands;
import com.fortify.cli.sc_dast.command.transfer.scanlogs.actions.SCDastTransferScanLogsActionsHandler;
import com.fortify.cli.sc_dast.command.transfer.scanlogs.options.SCDastTransferScanLogsOptions;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastTransferScanLogsCommands {
    private static final String NAME = "scan-logs";
    private static final String DESC = "DAST scan logs";

    private static final String _getDefaultOutputColumns() {
        return  "path";
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastTransferRootCommands.SCDastDownloadCommand.class)
    @Command(name = NAME, description = "Download " + DESC + " from SC DAST")
    public static final class Download extends AbstractSCDastUnirestRunnerCommand implements IOutputConfigSupplier {

        @ArgGroup(exclusive = false, heading = "Download logs from a specific scan:%n", order = 1)
        @Getter private SCDastTransferScanLogsOptions scanLogsOptions;

        @Mixin
        @Getter private OutputMixin OutputMixin;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest){
            SCDastTransferScanLogsActionsHandler actionsHandler = new SCDastTransferScanLogsActionsHandler(unirest);

            ObjectNode result = actionsHandler.downloadScanLogs(scanLogsOptions.getScanId(), scanLogsOptions.getFile());

            OutputMixin.write(result);

            return null;
        }

        @Override
		public OutputConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
		}
    }
}

