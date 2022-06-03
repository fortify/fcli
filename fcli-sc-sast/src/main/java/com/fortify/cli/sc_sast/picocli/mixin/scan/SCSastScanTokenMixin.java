package com.fortify.cli.sc_sast.picocli.mixin.scan;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Option;

@ReflectiveAccess
public class SCSastScanTokenMixin {
    @Option(names = {"-t","--token", "--job-token"}, description = "The scan job token", required = true)
    @Getter private String token;
}