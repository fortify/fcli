package com.fortify.cli.sc_dast.command.dast_scan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.picocli.annotation.SubcommandOf;
import com.fortify.cli.common.picocli.component.output.OutputOptionsHandler;
import com.fortify.cli.sc_dast.command.AbstractSCDastUnirestRunnerCommand;
import com.fortify.cli.sc_dast.command.crud.scan.actions.SCDastScanActionsHandler;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanCompleteOptions;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanDeleteOptions;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanPauseOptions;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanPublishOptions;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanResumeOptions;
import com.fortify.cli.sc_dast.command.dast_scan.options.SCDastScanStartOptions;

import io.micronaut.core.annotation.Order;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public class SCDastScanCommands {

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "start", description = "Starts DAST scan on ScanCentral DAST")
    @Order(SCDastScanCommandsOrderBy.START)
    public static final class Start extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Scan options:%n", order = 1)
        @Getter private SCDastScanStartOptions scanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.startScan(scanOptions.getJsonBody());
            outputOptionsHandler.write(response);

            return null;
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "pause", description = "Pauses a DAST scan on ScanCentral DAST")
    @Order(SCDastScanCommandsOrderBy.PAUSE)
    public static final class Pause extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Pause scan options:%n", order = 1)
        @Getter private SCDastScanPauseOptions pauseScanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.pauseScan(pauseScanOptions.getScanId());

            if(response != null) outputOptionsHandler.write(response);

            if(pauseScanOptions.isWaitPaused()){ actionsHandler.waitPaused(pauseScanOptions.getScanId(), pauseScanOptions.getWaitInterval()); }

            return null;
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "resume", description = "Resumes a DAST scan on ScanCentral DAST")
    @Order(SCDastScanCommandsOrderBy.RESUME)
    public static final class Resume extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Resume scan options:%n", order = 1)
        @Getter private SCDastScanResumeOptions resumeScanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.resumeScan(resumeScanOptions.getScanId());

            if(response != null) outputOptionsHandler.write(response);

            if(resumeScanOptions.isWaitResumed()){ actionsHandler.waitResumed(resumeScanOptions.getScanId(), resumeScanOptions.getWaitInterval()); }

            return null;
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "complete", description = "Completes a DAST scan on ScanCentral DAST")
    @Order(SCDastScanCommandsOrderBy.COMPLETE)
    public static final class Complete extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Complete scan options:%n", order = 1)
        @Getter private SCDastScanCompleteOptions completeScanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.completeScan(completeScanOptions.getScanId());

            if(response != null) outputOptionsHandler.write(response);

            if(completeScanOptions.isWaitCompleted()){ actionsHandler.waitCompleted(completeScanOptions.getScanId(), completeScanOptions.getWaitInterval()); }

            return null;
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "delete", description = "Deletes a DAST scan on ScanCentral DAST")
    @Order(SCDastScanCommandsOrderBy.DELETE)
    public static final class Delete extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Delete scan options:%n", order = 1)
        @Getter private SCDastScanDeleteOptions deleteScanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.deleteScan(deleteScanOptions.getScanId());

            if(response != null) outputOptionsHandler.write(response);

            return null;
        }
    }

    @ReflectiveAccess
    @SubcommandOf(SCDastScanRootCommands.SCDastCommand.class)
    @Command(name = "publish", description = "Publishes a DAST scan on ScanCentral DAST to SSC")
    @Order(SCDastScanCommandsOrderBy.PUBLISH)
    public static final class Publish extends AbstractSCDastUnirestRunnerCommand {
        @ArgGroup(exclusive = false, heading = "Publish scan options:%n", order = 1)
        @Getter private SCDastScanPublishOptions publishScanOptions;

        @Mixin private OutputOptionsHandler outputOptionsHandler;

        @SneakyThrows
        protected Void runWithUnirest(UnirestInstance unirest) {
            SCDastScanActionsHandler actionsHandler = new SCDastScanActionsHandler(unirest);
            JsonNode response = actionsHandler.publishScan(publishScanOptions.getScanId());

            if(response != null) outputOptionsHandler.write(response);

            return null;
        }
    }
}
