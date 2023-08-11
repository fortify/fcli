/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.config.connection.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.config.connection.connecttimeout.cli.cmd.ConnectTimeoutCommands;
import com.fortify.cli.config.connection.sockettimeout.cli.cmd.SocketTimeoutCommands;

import picocli.CommandLine.Command;

@Command(
        name = "connection",
        subcommands = {
            ConnectTimeoutCommands.class,
            SocketTimeoutCommands.class
        }
)
public class ConnectionCommands extends AbstractContainerCommand {
}
