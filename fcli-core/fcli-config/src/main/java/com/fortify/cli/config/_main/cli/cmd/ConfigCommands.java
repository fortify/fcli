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
package com.fortify.cli.config._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.config.language.cli.cmd.LanguageCommands;
import com.fortify.cli.config.proxy.cli.cmd.ProxyCommands;
import com.fortify.cli.config.publickey.cli.cmd.PublicKeyCommands;
import com.fortify.cli.config.truststore.cli.cmd.TrustStoreCommands;

import picocli.CommandLine.Command;

@Command(
        name = "config",
        aliases = "cfg",
        resourceBundle = "com.fortify.cli.config.i18n.ConfigMessages",
        subcommands = {
                ConfigClearCommand.class,
                LanguageCommands.class,
                ProxyCommands.class,
                PublicKeyCommands.class,
                TrustStoreCommands.class
        }
)
public class ConfigCommands extends AbstractContainerCommand {
}
