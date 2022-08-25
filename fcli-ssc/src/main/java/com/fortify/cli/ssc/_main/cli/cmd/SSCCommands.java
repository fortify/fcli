package com.fortify.cli.ssc._main.cli.cmd;

import com.fortify.cli.ssc.app.cli.cmd.SSCAppCommands;
import com.fortify.cli.ssc.appversion.cli.cmd.SSCAppVersionCommands;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.SSCAppVersionArtifactCommands;
import com.fortify.cli.ssc.appversion_attribute.cli.cmd.SSCAppVersionAttributeCommands;
import com.fortify.cli.ssc.attribute_definition.cli.cmd.SSCAttributeDefinitionCommands;
import com.fortify.cli.ssc.event.cli.cmd.SSCEventCommands;
import com.fortify.cli.ssc.plugin.cli.cmd.SSCPluginCommands;
import com.fortify.cli.ssc.report_template.cli.cmd.SSCReportTemplateCommands;
import com.fortify.cli.ssc.rest.cli.cmd.SSCRestCommand;
import com.fortify.cli.ssc.session.cli.cmd.SSCSessionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages",
        subcommands = {
                SSCSessionCommands.class,
                SSCRestCommand.class,
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
