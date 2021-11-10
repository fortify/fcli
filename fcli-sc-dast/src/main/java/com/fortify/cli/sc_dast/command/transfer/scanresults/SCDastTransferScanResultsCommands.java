package com.fortify.cli.sc_dast.command.transfer.scanresults;

import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.IOutputOptionsWriterConfigSupplier;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.common.picocli.component.output.OutputOptionsWriterConfig;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.SCDastCrudRootCommands.SCDastGetCommand;
import com.fortify.cli.sc_dast.command.transfer.SCDastTransferRootCommands;
import com.fortify.cli.sc_dast.command.transfer.scanresults.actions.SCDastTransferScanResultsActionsHandler;
import com.fortify.cli.sc_dast.command.transfer.scanresults.options.SCDastTransferScanResultsOptions;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastTransferScanResultsCommands {
    private static final String NAME = "scan-results";
    private static final String DESC = "DAST scan results";

    private static final String _getDefaultOutputColumns() {
        return  "lowCount:Low#" +
                "mediumCount:Medium#" +
                "highCount:High#" +
                "criticalCount:Critical";
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastTransferRootCommands.SCDastDownloadCommand.class)
    @Command(name = NAME, description = "Download " + DESC + " from SC DAST")
    public static final class Download extends AbstractSCDastUnirestRunnerCommand implements IOutputOptionsWriterConfigSupplier {

        @ArgGroup(exclusive = false, heading = "Download results from a specific scan:%n", order = 1)
        @Getter private SCDastTransferScanResultsOptions scanResultsOptions;

        @Mixin
        @Getter private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest){
            SCDastTransferScanResultsActionsHandler actionsHandler = new SCDastTransferScanResultsActionsHandler(unirest);

            actionsHandler.downloadScanResults(scanResultsOptions.getScanId(), scanResultsOptions.getFile());

            return null;
        }

        @Override
		public OutputOptionsWriterConfig getOutputOptionsWriterConfig() {
			return SCDastGetCommand.defaultOutputConfig().defaultColumns(_getDefaultOutputColumns());
		}
    }
}

