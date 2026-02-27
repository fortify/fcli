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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvExcludeMixin;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvOutputAsMixin;
import com.fortify.cli.tool.env.helper.ToolEnvContext;
import com.fortify.cli.tool.env.helper.ToolEnvOutputHelper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "github")
public final class ToolEnvGitHubCommand extends AbstractToolEnvCommand {
    private static final String GITHUB_ENV = "GITHUB_ENV";
    private static final String GITHUB_PATH = "GITHUB_PATH";

    @Mixin private ToolEnvExcludeMixin exclude;
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
        if (exclude.isIncludeVars()) {
            List<String> envLines = new ArrayList<>();
            for (ToolEnvContext context : contexts) {
                envLines.addAll(ToolEnvOutputHelper.gitHubEnvLines(context, exclude));
            }
            writeLinesToPath(envLines, requireGithubFile(GITHUB_ENV, "environment"), "GitHub environment output");
        }
        if (exclude.isIncludePath()) {
            List<String> pathLines = new ArrayList<>();
            for (ToolEnvContext context : contexts) {
                pathLines.addAll(ToolEnvOutputHelper.gitHubPathLines(context, exclude));
            }
            writeLinesToPath(pathLines, requireGithubFile(GITHUB_PATH, "PATH"), "GitHub PATH output");
        }
    }

    private void processWithOutputAs(List<ToolEnvContext> contexts, ToolEnvOutputAsMixin.OutputAs outputAsValue) {
        List<String> lines = new ArrayList<>();
        for (ToolEnvContext context : contexts) {
            lines.addAll(ToolEnvOutputHelper.echoRedirectLines(
                    ToolEnvOutputHelper.gitHubPathLines(context, exclude),
                    ToolEnvOutputHelper.gitHubPathTarget(outputAsValue)));
            lines.addAll(ToolEnvOutputHelper.echoRedirectLines(
                    ToolEnvOutputHelper.gitHubEnvLines(context, exclude),
                    ToolEnvOutputHelper.gitHubEnvTarget(outputAsValue)));
            lines.addAll(ToolEnvOutputHelper.outputAsLines(context, exclude, outputAsValue));
        }
        lines.forEach(System.out::println);
    }

    private static Path requireGithubFile(String envName, String description) {
        String value = StringUtils.trimToNull(EnvHelper.env(envName));
        if (value == null) {
            throw new FcliSimpleException(String.format("Environment variable %s must be set when generating GitHub %s output", envName, description));
        }
        return Path.of(value);
    }
}
