package com.fortify.cli.dast.command.entity.scdast.scanstatus.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan settings list
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastGetScanStatusOptions {

    @Option(names = {"-id", "--scan-id"}, description = "The scan id")
    @Getter private int scanId;


}
