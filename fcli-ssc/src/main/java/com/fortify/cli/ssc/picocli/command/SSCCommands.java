package com.fortify.cli.ssc.picocli.command;

import com.fortify.cli.ssc.picocli.command.api.SSCApiCommand;
import com.fortify.cli.ssc.picocli.command.app.SSCApplicationCommands;
import com.fortify.cli.ssc.picocli.command.appversion.SSCApplicationVersionCommands;
import com.fortify.cli.ssc.picocli.command.appversion_artifact.SSCApplicationVersionArtifactCommands;
import com.fortify.cli.ssc.picocli.command.appversion_attribute.SSCApplicationVersionAttributeCommands;
import com.fortify.cli.ssc.picocli.command.event.SSCEventCommands;
import com.fortify.cli.ssc.picocli.command.plugin.SSCPluginCommands;
import com.fortify.cli.ssc.picocli.command.session.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages",
        subcommands = {
                SSCSessionCommands.class,
                SSCApiCommand.class,
                SSCApplicationCommands.class,
                SSCApplicationVersionCommands.class,
                SSCApplicationVersionArtifactCommands.class,
                SSCApplicationVersionAttributeCommands.class,
                SSCPluginCommands.class,
                SSCEventCommands.class
        }
)
public class SSCCommands {
        public SSCCommands(){
                System.setProperty("productName", "Fortify SSC");
        }
}
