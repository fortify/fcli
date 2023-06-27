/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.variable.DefaultVariablePropertyName;
import com.fortify.cli.ssc.entity.appversion_artifact.cli.cmd.import_debricked.SSCAppVersionArtifactImportDebrickedCommand;

import picocli.CommandLine.Command;

@Command(
        name = "appversion-artifact",
        subcommands = {
            SSCAppVersionArtifactApproveCommand.class,
            SSCAppVersionArtifactDeleteCommand.class,
            SSCAppVersionArtifactDownloadByIdCommand.class,
            SSCAppVersionArtifactDownloadStateCommand.class,
            SSCAppVersionArtifactGetCommand.class,
            SSCAppVersionArtifactImportDebrickedCommand.class,
            SSCAppVersionArtifactListCommand.class,
            SSCAppVersionArtifactPurgeByIdCommand.class,
            SSCAppVersionArtifactPurgeOlderThanCommand.class,
            SSCAppVersionArtifactUploadCommand.class,
            SSCAppVersionArtifactWaitForCommand.class
        }
)
@DefaultVariablePropertyName("id")
public class SSCAppVersionArtifactCommands extends AbstractFortifyCLICommand {
}
