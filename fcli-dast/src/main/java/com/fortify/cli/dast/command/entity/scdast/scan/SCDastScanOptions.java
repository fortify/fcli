package com.fortify.cli.dast.command.entity.scdast.scan;

import com.fortify.cli.common.command.auth.login.AuthLoginCommand;
import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

/**
 * Configure options for running SC DAST Scan
 *
 * @author Ruud Senden
 */
@ReflectiveAccess
public class SCDastScanOptions {

    @Option(names = {"-cicd","--cicd-roken"}, description = "The CI/CD token to run the scan with.", required = true)
    @Getter private String cicdToken;

    @Option(names = {"-n","--scan-name"}, description = "The name of the SC DAST scan")
    @Getter private String scanName;
}
