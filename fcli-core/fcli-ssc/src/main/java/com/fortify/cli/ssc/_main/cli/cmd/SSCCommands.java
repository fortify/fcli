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

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.ssc._common.session.cli.cmd.SSCSessionCommands;
import com.fortify.cli.ssc.alert.cli.cmd.SSCAlertCommands;
import com.fortify.cli.ssc.app.cli.cmd.SSCAppCommands;
import com.fortify.cli.ssc.appversion.cli.cmd.SSCAppVersionCommands;
import com.fortify.cli.ssc.appversion_user.cli.cmd.SSCAppVersionUserCommands;
import com.fortify.cli.ssc.artifact.cli.cmd.SSCArtifactCommands;
import com.fortify.cli.ssc.attribute.cli.cmd.SSCAttributeCommands;
import com.fortify.cli.ssc.issue.cli.cmd.SSCIssueCommands;
import com.fortify.cli.ssc.performance_indicator.cli.cmd.SSCPerformanceIndicatorCommands;
import com.fortify.cli.ssc.plugin.cli.cmd.SSCPluginCommands;
import com.fortify.cli.ssc.report.cli.cmd.SSCReportCommands;
import com.fortify.cli.ssc.rest.cli.cmd.SSCRestCommands;
import com.fortify.cli.ssc.role.cli.cmd.SSCRoleCommands;
import com.fortify.cli.ssc.system_state.cli.cmd.SSCSystemStateCommands;
import com.fortify.cli.ssc.token.cli.cmd.SSCTokenCommands;
import com.fortify.cli.ssc.user.cli.cmd.SSCUserCommands;
import com.fortify.cli.ssc.variable.cli.cmd.SSCVariableCommands;

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
                SSCAlertCommands.class,
                SSCAppCommands.class,
                SSCAppVersionCommands.class,
                SSCAppVersionUserCommands.class,
                SSCArtifactCommands.class,
                SSCAttributeCommands.class,
                SSCIssueCommands.class,
                SSCPerformanceIndicatorCommands.class,
                SSCVariableCommands.class,
                SSCPluginCommands.class,
                SSCReportCommands.class,
                SSCRoleCommands.class,
                SSCSystemStateCommands.class,
                SSCTokenCommands.class,
                SSCUserCommands.class,
                SSCRestCommands.class,
        }
)
public class SSCCommands extends AbstractContainerCommand {}
