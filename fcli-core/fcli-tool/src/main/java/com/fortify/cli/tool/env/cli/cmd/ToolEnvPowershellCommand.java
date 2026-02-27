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
package com.fortify.cli.tool.env.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvExcludeMixin;
import com.fortify.cli.tool.env.helper.ToolEnvContext;
import com.fortify.cli.tool.env.helper.ToolEnvOutputHelper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "powershell", aliases = {"pwsh"})
public final class ToolEnvPowershellCommand extends AbstractToolEnvCommand {
    @Mixin private ToolEnvExcludeMixin exclude;
    @Mixin private CommonOptionMixins.OptionalFile outputFile;

    @Override
    protected void process(List<ToolEnvContext> contexts) {
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            lines.addAll(ToolEnvOutputHelper.pwshLines(context, exclude));
        }
        writeLines(lines, outputFile.getFile(), "PowerShell environment output");
    }
}
