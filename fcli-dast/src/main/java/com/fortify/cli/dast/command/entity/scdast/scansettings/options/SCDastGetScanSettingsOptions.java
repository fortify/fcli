package com.fortify.cli.dast.command.entity.scdast.scansettings.options;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for retrieving SC DAST Scan settings list
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastGetScanSettingsOptions {

    @Option(names = {"-id", "--scan-settings-id"}, description = "The scan settings id")
    @Getter private String scanSettingsId;


}
