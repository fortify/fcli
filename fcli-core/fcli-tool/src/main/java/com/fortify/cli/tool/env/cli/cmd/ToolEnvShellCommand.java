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
package com.fortify.cli.tool.env.cli.cmd;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.cli.mixin.CommonOptionMixins;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvExcludeMixin;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "shell")
public final class ToolEnvShellCommand extends AbstractToolEnvCommand {
    private static final String PATH_TEMPLATE = "{binDir != null ? 'export PATH=\"' + binDir + ':$PATH\"' : ''}";
    private static final String HOME_TEMPLATE = "{installDir != null ? 'export ' + defaultEnvPrefix + '_HOME=\"' + installDir + '\"' : ''}";
    private static final String CMD_TEMPLATE = "{cmd != null ? 'export ' + defaultEnvPrefix + '_CMD=\"' + cmd + '\"' : ''}";

    @Mixin private ToolEnvExcludeMixin exclude;
    @Mixin private CommonOptionMixins.OptionalFile outputFile;

    @Override
    protected void process(List<ToolEnvContext> contexts) {
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            if (exclude.isIncludePath()) {
                addIfNotBlank(lines, renderTemplate(PATH_TEMPLATE, context));
            }
            if (exclude.isIncludeVars()) {
                addIfNotBlank(lines, renderTemplate(HOME_TEMPLATE, context));
                addIfNotBlank(lines, renderTemplate(CMD_TEMPLATE, context));
            }
        }
        writeLines(lines, outputFile.getFile(), "shell environment output");
    }
}
