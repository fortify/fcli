/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.agent._main.cli.cmd;

import static com.fortify.cli.common.cli.util.FcliModuleCategories.UTIL;

import com.fortify.cli.agent.mcp.cli.cmd.AgentMCPCommands;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.cli.util.FcliModuleCategory;

import picocli.CommandLine.Command;

@FcliModuleCategory(UTIL)
@Command(
        name = "agent",
        resourceBundle = "com.fortify.cli.agent.i18n.AgentMessages",
        subcommands = {
            AgentMCPCommands.class
        }
)
public class AgentCommands extends AbstractContainerCommand {}
