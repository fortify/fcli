package com.fortify.cli.ssc._main.cli.cmd;

import com.fortify.cli.ssc.activity_feed.cli.cmd.SSCActivityFeedCommands;
import com.fortify.cli.ssc.alert.cli.cmd.SSCAlertCommands;
import com.fortify.cli.ssc.alert_definition.cli.cmd.SSCAlertDefinitionCommands;
import com.fortify.cli.ssc.app.cli.cmd.SSCAppCommands;
import com.fortify.cli.ssc.appversion.cli.cmd.SSCAppVersionCommands;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.SSCAppVersionArtifactCommands;
import com.fortify.cli.ssc.appversion_attribute.cli.cmd.SSCAppVersionAttributeCommands;
import com.fortify.cli.ssc.appversion_auth_entity.cli.cmd.SSCAppVersionAuthEntityCommands;
import com.fortify.cli.ssc.appversion_vuln.cli.cmd.SSCAppVersionVulnCommands;
import com.fortify.cli.ssc.attribute_definition.cli.cmd.SSCAttributeDefinitionCommands;
import com.fortify.cli.ssc.auth_entity.cli.cmd.SSCAuthEntityCommands;
import com.fortify.cli.ssc.event.cli.cmd.SSCEventCommands;
import com.fortify.cli.ssc.issue_template.cli.cmd.SSCIssueTemplateCommands;
import com.fortify.cli.ssc.job.cli.cmd.SSCJobCommands;
import com.fortify.cli.ssc.plugin.cli.cmd.SSCPluginCommands;
import com.fortify.cli.ssc.report_template.cli.cmd.SSCReportTemplateCommands;
import com.fortify.cli.ssc.rest.cli.cmd.SSCRestCommand;
import com.fortify.cli.ssc.seed_bundle.cli.cmd.SSCSeedBundleCommands;
import com.fortify.cli.ssc.session.cli.cmd.SSCSessionCommands;
import com.fortify.cli.ssc.token.cli.cmd.SSCTokenCommands;
import com.fortify.cli.ssc.token_definition.cli.cmd.SSCTokenDefinitionCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages",
        subcommands = {
                SSCSessionCommands.class,
                SSCRestCommand.class,
                SSCActivityFeedCommands.class,
                SSCAlertDefinitionCommands.class,
                SSCAlertCommands.class,
                SSCAppCommands.class,
                SSCAppVersionCommands.class,
                SSCAppVersionArtifactCommands.class,
                SSCAppVersionAttributeCommands.class,
                SSCAppVersionAuthEntityCommands.class,
                SSCAppVersionVulnCommands.class,
                SSCAttributeDefinitionCommands.class,
                SSCAuthEntityCommands.class,
                SSCEventCommands.class,
                SSCIssueTemplateCommands.class,
                SSCJobCommands.class,
                SSCPluginCommands.class,
                SSCReportTemplateCommands.class,
                SSCSeedBundleCommands.class,
                SSCTokenCommands.class,
                SSCTokenDefinitionCommands.class
        }
)
public class SSCCommands {
        public SSCCommands(){
                System.setProperty("productName", "Fortify SSC");
        }
}
