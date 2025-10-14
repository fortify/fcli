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
package com.fortify.cli.aviator._main.cli.cmd;

import com.fortify.cli.aviator._common.config.admin.cli.cmd.AviatorAdminConfigCommands;
import com.fortify.cli.aviator._common.session.user.cli.cmd.AviatorUserSessionCommands;
import com.fortify.cli.aviator.app.cli.cmd.AviatorAppCommands;
import com.fortify.cli.aviator.entitlement.cli.cmd.AviatorEntitlementCommands;
import com.fortify.cli.aviator.fod.cli.cmd.AviatorFoDCommands;
import com.fortify.cli.aviator.ssc.cli.cmd.AviatorSSCCommands;
import com.fortify.cli.aviator.token.cli.cmd.AviatorTokenCommands;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;

import picocli.CommandLine.Command;

@Command(
        name = "aviator",
        resourceBundle = "com.fortify.cli.aviator.i18n.AviatorMessages",
        subcommands = {
                // This list of product subcommands should be in alphabetical
                // order, except for:
                // - session command (should be the first command, as it is a 
                //   prerequisite for all other commands)
                // - rest command (should be the last command, as it's a low-level
                //   command and looks better in the usage command list, as usually 
                //   'rest' has a different header ('Interact with' compared to most 
                //   other commands ('Manage').
                AviatorAdminConfigCommands.class,
                AviatorUserSessionCommands.class,
                AviatorAppCommands.class,
                AviatorEntitlementCommands.class,
                AviatorSSCCommands.class,
//                AviatorFoDCommands.class,
                AviatorTokenCommands.class,
        }
)
public class AviatorCommands extends AbstractContainerCommand {}
