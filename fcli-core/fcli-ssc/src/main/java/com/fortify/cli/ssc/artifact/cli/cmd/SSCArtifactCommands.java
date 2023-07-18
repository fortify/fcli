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
package com.fortify.cli.ssc.artifact.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.artifact.cli.cmd.import_debricked.SSCArtifactImportDebrickedCommand;

import picocli.CommandLine.Command;

@Command(
        name = "artifact",
        subcommands = {
            SSCArtifactApproveCommand.class,
            SSCArtifactDeleteCommand.class,
            SSCArtifactDownloadCommand.class,
            SSCArtifactGetCommand.class,
            SSCArtifactImportDebrickedCommand.class,
            SSCArtifactListCommand.class,
            SSCArtifactPurgeCommand.class,
            SSCArtifactUploadCommand.class,
            SSCArtifactWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SSCArtifactCommands extends AbstractFortifyCLICommand {
}
