package com.fortify.cli.dast.command.crud.scdast.scanresults.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan results
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanResultsOptions {

    @Option(names = {"-id", "--scan-id"}, description = "The scan id", required = true)
    @Getter private int scanId;

    @Option(names = {"-w", "--wait", "--wait-completion"}, defaultValue = "false",
            description = "Wait while the scan is Queued, Pending, or Running. Then displays scan results. ")
    @Getter private boolean waitCompletion;

    @Option(names = {"-wi", "--wait-interval"}, defaultValue = "30",
            description = "When waiting for completion, how long between to poll, in seconds", showDefaultValue = CommandLine.Help.Visibility.ALWAYS)
    @Getter private int waitInterval;

}
