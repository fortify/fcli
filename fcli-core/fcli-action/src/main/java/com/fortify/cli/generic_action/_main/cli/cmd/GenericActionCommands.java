/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.generic_action._main.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionAsciidocCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionGetCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionHelpCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionImportCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionListCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionResetCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionRunCommand;
import com.fortify.cli.generic_action.action.cli.cmd.GenericActionSignCommand;

import picocli.CommandLine.Command;

@Command(
        name = "action",
        resourceBundle = "com.fortify.cli.generic_action.i18n.GenericActionMessages",
        subcommands = {
                GenericActionAsciidocCommand.class,
                GenericActionGetCommand.class,
                GenericActionHelpCommand.class,
                GenericActionImportCommand.class,
                GenericActionListCommand.class,
                GenericActionResetCommand.class,
                GenericActionRunCommand.class,
                GenericActionSignCommand.class, 
        }
)
public class GenericActionCommands extends AbstractContainerCommand {}
