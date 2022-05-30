package com.fortify.cli.fod.picocli.command.application;

import com.fortify.cli.fod.picocli.command.AbstractFoDUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;

@ReflectiveAccess
@CommandLine.Command(name = "delete", description = "Delete an application from FoD.")
public class FoDApplicationDeleteCommand extends AbstractFoDUnirestRunnerCommand {
    @SneakyThrows
    protected Void runWithUnirest(UnirestInstance unirest) {
        System.err.println("ERROR: Not yet implemented");
        return null;
    }
}
