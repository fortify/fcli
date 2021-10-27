package com.fortify.cli.dast.command.entity.scdast.scanresults.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan list
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastGetScanResultsOptions {

    @Option(names = {"-id", "--scan-id"}, description = "The scan id", required = true)
    @Getter private String scanId;


}
