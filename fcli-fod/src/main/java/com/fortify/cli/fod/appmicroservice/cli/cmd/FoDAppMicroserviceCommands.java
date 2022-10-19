package com.fortify.cli.fod.appmicroservice.cli.cmd;

import com.fortify.cli.common.variable.PredefinedVariable;
import com.fortify.cli.fod.appmicroservice.cli.cmd.*;
import picocli.CommandLine;

@CommandLine.Command(name = "microservice",
        aliases = {"application-microservice", "app-ms"},
        subcommands = {
                FoDAppMicroserviceCreateCommand.class,
                FoDAppMicroserviceListCommand.class,
                //FoDAppMicroserviceGetCommand.class,
                FoDAppMicroserviceUpdateCommand.class,
                FoDAppMicroserviceDeleteCommand.class
        }
)
@PredefinedVariable(name = "currentMicroservice", field = "id")
public class FoDAppMicroserviceCommands {
}
