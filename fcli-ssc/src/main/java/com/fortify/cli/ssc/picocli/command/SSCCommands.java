package com.fortify.cli.ssc.picocli.command;

import com.fortify.cli.ssc.picocli.command.api.SSCApiCommand;
import com.fortify.cli.ssc.picocli.command.app.SSCAppCommands;
import com.fortify.cli.ssc.picocli.command.appversion.SSCAppVersionCommands;
import com.fortify.cli.ssc.picocli.command.appversion_artifact.SSCAppVersionArtifactCommands;
import com.fortify.cli.ssc.picocli.command.appversion_attribute.SSCAppVersionAttributeCommands;
import com.fortify.cli.ssc.picocli.command.attribute_definition.SSCAttributeDefinitionCommands;
import com.fortify.cli.ssc.picocli.command.event.SSCEventCommands;
import com.fortify.cli.ssc.picocli.command.plugin.SSCPluginCommands;
import com.fortify.cli.ssc.picocli.command.report_template.SSCReportTemplateCommands;
import com.fortify.cli.ssc.picocli.command.session.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages",
        subcommands = {
                SSCSessionCommands.class,
                SSCApiCommand.class,
                SSCAppCommands.class,
                SSCAppVersionCommands.class,
                SSCAppVersionArtifactCommands.class,
                SSCAppVersionAttributeCommands.class,
                SSCAttributeDefinitionCommands.class,
                SSCPluginCommands.class,
                SSCReportTemplateCommands.class,
                SSCEventCommands.class
        }
)
public class SSCCommands {
        public SSCCommands(){
                System.setProperty("productName", "Fortify SSC");
        }
}
