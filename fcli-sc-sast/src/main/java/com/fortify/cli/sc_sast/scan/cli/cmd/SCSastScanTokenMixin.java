package com.fortify.cli.sc_sast.scan.cli.cmd;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

@ReflectiveAccess
public class SCSastScanTokenMixin {
    @Option(names = {"-t","--token", "--job-token"}, required = true)
    @Getter private String token;
}