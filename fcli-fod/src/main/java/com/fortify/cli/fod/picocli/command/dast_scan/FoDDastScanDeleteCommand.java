package com.fortify.cli.fod.picocli.command.dast_scan;

import com.fortify.cli.fod.picocli.command.AbstractFoDUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@ReflectiveAccess
@CommandLine.Command(name = "delete")
public class FoDDastScanDeleteCommand extends AbstractFoDUnirestRunnerCommand {
    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        System.err.println("ERROR: Not yet implemented");
        return null;
    }
}
