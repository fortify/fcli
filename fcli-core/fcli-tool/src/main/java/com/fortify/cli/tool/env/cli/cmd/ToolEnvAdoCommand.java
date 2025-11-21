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

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.tool.env.cli.mixin.ToolEnvExcludeMixin;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "ado")
public final class ToolEnvAdoCommand extends AbstractToolEnvCommand {
    @Mixin private ToolEnvExcludeMixin exclude;

    @Override
    protected void process(List<ToolEnvContext> contexts) {
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            if (exclude.isIncludePath() && StringUtils.isNotBlank(context.binDir())) {
                lines.add("##vso[task.prependpath]" + context.binDir());
            }
            if (exclude.isIncludeVars()) {
                if (StringUtils.isNotBlank(context.installDir())) {
                    lines.add(String.format("##vso[task.setvariable variable=%s_HOME]%s", context.envPrefix(), context.installDir()));
                }
                if (StringUtils.isNotBlank(context.cmd())) {
                    lines.add(String.format("##vso[task.setvariable variable=%s_CMD]%s", context.envPrefix(), context.cmd()));
                }
            }
        }
        lines.forEach(System.out::println);
    }
}
