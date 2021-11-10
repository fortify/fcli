package com.fortify.cli.sc_dast.command.transfer.scanresults.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.File;

/**
 * Configure options for retrieving SC DAST Scan results
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastTransferScanResultsOptions {

    @Option(names = {"-i","--id", "--scan-id"}, description = "The scan id", required = true)
    @Getter private int scanId;

    @Option(names = {"-f", "--file", "--output-file"}, description = "The output file to save the scan results in.", required = true)
    @Getter private File file;
}
