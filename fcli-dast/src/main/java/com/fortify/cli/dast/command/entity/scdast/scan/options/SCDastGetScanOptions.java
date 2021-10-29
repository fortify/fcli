package com.fortify.cli.dast.command.entity.scdast.scan.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan list
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastGetScanOptions {

    @Option(names = {"-id", "--scan-id"}, description = "The scan id")
    @Getter private String scanId;


}
