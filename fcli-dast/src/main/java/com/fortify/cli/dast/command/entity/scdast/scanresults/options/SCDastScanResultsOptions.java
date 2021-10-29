package com.fortify.cli.dast.command.entity.scdast.scanresults.options;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.util.JsonNodeFilterHandler;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.SCDastScanStatusCommands;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.actions.SCDastScanStatusActionsHandler;
import com.fortify.cli.dast.command.entity.scdast.scanstatus.options.SCDastGetScanStatusOptions;
import com.fortify.cli.dast.rest.unirest.SCDastUnirestRunner;
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.Set;

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
