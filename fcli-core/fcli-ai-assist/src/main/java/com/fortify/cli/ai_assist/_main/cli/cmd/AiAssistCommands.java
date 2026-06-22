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
package com.fortify.cli.ai_assist._main.cli.cmd;

import static com.fortify.cli.common.cli.util.FcliModuleCategories.UTIL;

import com.fortify.cli.ai_assist.extensions.cli.cmd.AiAssistExtensionsCommands;
import com.fortify.cli.ai_assist.mcp.cli.cmd.AiAssistMCPCommands;
import com.fortify.cli.common.cli.cmd.AbstractContainerCommand;
import com.fortify.cli.common.cli.util.FcliModuleCategory;

import picocli.CommandLine.Command;

@FcliModuleCategory(UTIL)
@Command(
        name = "ai-assist",
        aliases = {"ai"},
        resourceBundle = "com.fortify.cli.ai_assist.i18n.AiAssistMessages",
        subcommands = {
            AiAssistExtensionsCommands.class,
            AiAssistMCPCommands.class
        }
)
public class AiAssistCommands extends AbstractContainerCommand {}
