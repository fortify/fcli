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
import com.fortify.cli.tool.env.cli.mixin.ToolEnvOutputAsMixin;
import com.fortify.cli.tool.env.helper.ToolEnvContext;
import com.fortify.cli.tool.env.helper.ToolEnvOutputHelper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "gitlab")
public final class ToolEnvGitLabCommand extends AbstractToolEnvCommand {
    @Mixin private ToolEnvExcludeMixin exclude;
    @Mixin private CommonOptionMixins.RequiredFile fileMixin;
    @Mixin private ToolEnvOutputAsMixin outputAs;

    @Override
    protected void process(List<ToolEnvContext> contexts) {
        if (outputAs.hasOutputAs()) {
            processWithOutputAs(contexts, outputAs.getOutputAs());
        } else {
            processNative(contexts);
        }
    }

    private void processNative(List<ToolEnvContext> contexts) {
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            lines.addAll(ToolEnvOutputHelper.gitLabLines(context, exclude));
        }
        writeLines(lines, fileMixin.getFile(), "GitLab environment output");
    }

    private void processWithOutputAs(List<ToolEnvContext> contexts, ToolEnvOutputAsMixin.OutputAs outputAsValue) {
        String filePath = fileMixin.getFile().getAbsolutePath();
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            lines.addAll(ToolEnvOutputHelper.echoRedirectLines(
                    ToolEnvOutputHelper.gitLabLines(context, exclude), filePath));
            lines.addAll(ToolEnvOutputHelper.outputAsLines(context, exclude, outputAsValue));
        }
        lines.forEach(System.out::println);
    }
}
