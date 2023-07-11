/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.ssc._common.session.cli.cmd.SSCSessionCommands;
import com.fortify.cli.ssc.activity_feed.cli.cmd.SSCActivityFeedCommands;
import com.fortify.cli.ssc.alert.cli.cmd.SSCAlertCommands;
import com.fortify.cli.ssc.alert_definition.cli.cmd.SSCAlertDefinitionCommands;
import com.fortify.cli.ssc.app.cli.cmd.SSCAppCommands;
import com.fortify.cli.ssc.appversion.cli.cmd.SSCAppVersionCommands;
import com.fortify.cli.ssc.appversion_artifact.cli.cmd.SSCAppVersionArtifactCommands;
import com.fortify.cli.ssc.appversion_attribute.cli.cmd.SSCAppVersionAttributeCommands;
import com.fortify.cli.ssc.appversion_filterset.cli.cmd.SSCAppVersionFilterSetCommands;
import com.fortify.cli.ssc.appversion_user.cli.cmd.SSCAppVersionAuthEntityCommands;
import com.fortify.cli.ssc.appversion_vuln.cli.cmd.SSCAppVersionVulnCommands;
import com.fortify.cli.ssc.attribute_definition.cli.cmd.SSCAttributeDefinitionCommands;
import com.fortify.cli.ssc.event.cli.cmd.SSCEventCommands;
import com.fortify.cli.ssc.issue_template.cli.cmd.SSCIssueTemplateCommands;
import com.fortify.cli.ssc.job.cli.cmd.SSCJobCommands;
import com.fortify.cli.ssc.plugin.cli.cmd.SSCPluginCommands;
import com.fortify.cli.ssc.report_template.cli.cmd.SSCReportTemplateCommands;
import com.fortify.cli.ssc.rest.cli.cmd.SSCRestCommands;
import com.fortify.cli.ssc.role.cli.cmd.SSCRoleCommands;
import com.fortify.cli.ssc.role_permission.cli.cmd.SSCRolePermissionCommands;
import com.fortify.cli.ssc.seed_bundle.cli.cmd.SSCSeedBundleCommands;
import com.fortify.cli.ssc.token.cli.cmd.SSCTokenCommands;
import com.fortify.cli.ssc.token_definition.cli.cmd.SSCTokenDefinitionCommands;
import com.fortify.cli.ssc.user.cli.cmd.SSCAuthEntityCommands;

import picocli.CommandLine.Command;

@Command(
        name = "ssc",
        resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages",
        subcommands = {
                // This list of product subcommands should be in alphabetical
                // order, except for:
                // - session command (should be the first command, as it is a 
                //   prerequisite for all other commands)
                // - rest command (should be the last command, as it's a low-level
                //   command and looks better in the usage command list, as usually 
                //   'rest' has a different header ('Interact with' compared to most 
                //   other commands ('Manage').
                SSCSessionCommands.class,
                SSCActivityFeedCommands.class,
                SSCAlertDefinitionCommands.class,
                SSCAlertCommands.class,
                SSCAppCommands.class,
                SSCAppVersionCommands.class,
                SSCAppVersionArtifactCommands.class,
                SSCAppVersionAttributeCommands.class,
                SSCAppVersionAuthEntityCommands.class,
                SSCAppVersionFilterSetCommands.class,
                SSCAppVersionVulnCommands.class,
                SSCAttributeDefinitionCommands.class,
                SSCAuthEntityCommands.class,
                SSCEventCommands.class,
                SSCIssueTemplateCommands.class,
                SSCJobCommands.class,
                SSCPluginCommands.class,
                SSCReportTemplateCommands.class,
                SSCRoleCommands.class,
                SSCRolePermissionCommands.class,
                SSCSeedBundleCommands.class,
                SSCTokenCommands.class,
                SSCTokenDefinitionCommands.class,
                SSCRestCommands.class,
        }
)
public class SSCCommands extends AbstractFortifyCLICommand {}
