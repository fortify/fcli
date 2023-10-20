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
package com.fortify.cli.ssc.access_control.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "access-control",
        aliases = "ac",
        subcommands = {
                SSCTokenDefinitionListCommand.class,
                SSCTokenCreateCommand.class,
                SSCTokenListCommand.class,
                SSCTokenRevokeCommand.class,
                SSCTokenUpdateCommand.class,
                SSCRoleCreateCommand.class,
                SSCRoleDeleteCommand.class,
                SSCRoleGetCommand.class,
                SSCRoleListCommand.class,
                SSCPermissionGetCommand.class,
                SSCPermissionListCommand.class,
                SSCUserCreateLocalCommand.class,
                SSCUserDeleteCommand.class,
                SSCUserGetCommand.class,
                SSCUserListCommand.class,
                SSCAppVersionUserListCommand.class
        }

)
public class SSCAccessControlCommands extends AbstractContainerCommand {
}
