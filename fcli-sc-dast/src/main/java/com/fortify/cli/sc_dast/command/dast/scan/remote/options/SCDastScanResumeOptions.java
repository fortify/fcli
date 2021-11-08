package com.fortify.cli.sc_dast.command.dast.scan.remote.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Configure options for running SC DAST Scan
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanResumeOptions {

    @Option(names = {"-i","--id", "--scan-id"}, description = "The scan id.", required = true)
    @Getter private int scanId;

    @Option(names = {"-w", "--wait", "--wait-resumed"}, defaultValue = "false",
            description = "Wait until the scan is running")
    @Getter private boolean waitResumed;

    @Option(names = {"--interval", "--wait-interval"}, defaultValue = "30",
            description = "When waiting for completion, how long between to poll, in seconds", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    @Getter private int waitInterval;
}
