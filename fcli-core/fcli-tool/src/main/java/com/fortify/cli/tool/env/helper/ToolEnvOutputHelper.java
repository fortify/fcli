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
package com.fortify.cli.tool.env.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.tool.env.cli.mixin.ToolEnvExcludeMixin;
import com.fortify.cli.tool.env.cli.mixin.ToolEnvOutputAsMixin.OutputAs;

/**
 * Utility methods for generating environment-setup output lines across the
 * various {@code fcli tool env} sub-commands.  Each method produces a list of
 * ready-to-print strings for a single {@link ToolEnvContext}; callers collect
 * into a combined list and write/print it themselves.
 */
public final class ToolEnvOutputHelper {
    private ToolEnvOutputHelper() {}

    // --- Native output generators -----------------------------------------------

    /** ADO {@code ##vso} logging commands. */
    public static List<String> adoLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludePath() && StringUtils.isNotBlank(ctx.binDir())) {
            lines.add("##vso[task.prependpath]" + ctx.binDir());
        }
        if (exclude.isIncludeVars()) {
            if (StringUtils.isNotBlank(ctx.installDir())) {
                lines.add(String.format("##vso[task.setvariable variable=%s_HOME]%s", ctx.envPrefix(), ctx.installDir()));
            }
            if (StringUtils.isNotBlank(ctx.cmd())) {
                lines.add(String.format("##vso[task.setvariable variable=%s_CMD]%s", ctx.envPrefix(), ctx.cmd()));
            }
        }
        return lines;
    }

    /** POSIX shell {@code export} statements. */
    public static List<String> shellLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludePath() && StringUtils.isNotBlank(ctx.binDir())) {
            lines.add("export PATH=\"" + ctx.binDir() + ":$PATH\"");
        }
        if (exclude.isIncludeVars()) {
            if (StringUtils.isNotBlank(ctx.installDir())) {
                lines.add("export " + ctx.envPrefix() + "_HOME=\"" + ctx.installDir() + "\"");
            }
            if (StringUtils.isNotBlank(ctx.cmd())) {
                lines.add("export " + ctx.envPrefix() + "_CMD=\"" + ctx.cmd() + "\"");
            }
        }
        return lines;
    }

    /** PowerShell {@code $env:} assignment statements. */
    public static List<String> pwshLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludePath() && StringUtils.isNotBlank(ctx.binDir())) {
            lines.add("$env:PATH = \"" + ctx.binDir() + ";\" + $env:PATH");
        }
        if (exclude.isIncludeVars()) {
            if (StringUtils.isNotBlank(ctx.installDir())) {
                lines.add("$env:" + ctx.envPrefix() + "_HOME = \"" + ctx.installDir() + "\"");
            }
            if (StringUtils.isNotBlank(ctx.cmd())) {
                lines.add("$env:" + ctx.envPrefix() + "_CMD = \"" + ctx.cmd() + "\"");
            }
        }
        return lines;
    }

    /** GitLab {@code KEY=VALUE} entries (written to the CI env file). */
    public static List<String> gitLabLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludePath() && StringUtils.isNotBlank(ctx.binDir())) {
            lines.add(String.format("PATH=\"%s%s$PATH\"", ctx.binDir(), File.pathSeparator));
        }
        if (exclude.isIncludeVars()) {
            if (StringUtils.isNotBlank(ctx.installDir())) {
                lines.add(String.format("%s_HOME=\"%s\"", ctx.envPrefix(), ctx.installDir()));
            }
            if (StringUtils.isNotBlank(ctx.cmd())) {
                lines.add(String.format("%s_CMD=\"%s\"", ctx.envPrefix(), ctx.cmd()));
            }
        }
        return lines;
    }

    /** GitHub {@code GITHUB_ENV} entries ({@code KEY=VALUE} pairs). */
    public static List<String> gitHubEnvLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludeVars()) {
            if (StringUtils.isNotBlank(ctx.installDir())) {
                lines.add(ctx.envPrefix() + "_HOME=" + ctx.installDir());
            }
            if (StringUtils.isNotBlank(ctx.cmd())) {
                lines.add(ctx.envPrefix() + "_CMD=" + ctx.cmd());
            }
        }
        return lines;
    }

    /** GitHub {@code GITHUB_PATH} entries (one directory per line). */
    public static List<String> gitHubPathLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude) {
        var lines = new ArrayList<String>();
        if (exclude.isIncludePath() && StringUtils.isNotBlank(ctx.binDir())) {
            lines.add(ctx.binDir());
        }
        return lines;
    }

    // --- Echo/redirect wrappers -------------------------------------------------

    /** Wraps each line in {@code echo '<line>'}. */
    public static List<String> echoLines(List<String> lines) {
        return lines.stream().map(l -> "echo '" + l + "'").toList();
    }

    /** Wraps each line in {@code echo '<line>' >> <target>}. */
    public static List<String> echoRedirectLines(List<String> lines, String target) {
        return lines.stream().map(l -> "echo '" + l + "' >> " + target).toList();
    }

    // --- OutputAs helpers -------------------------------------------------------

    /** Returns shell or pwsh lines depending on the requested {@link OutputAs}. */
    public static List<String> outputAsLines(ToolEnvContext ctx, ToolEnvExcludeMixin exclude, OutputAs outputAs) {
        return switch (outputAs) {
            case shell -> shellLines(ctx, exclude);
            case pwsh -> pwshLines(ctx, exclude);
        };
    }

    /** GitHub {@code GITHUB_ENV} target variable for the given {@link OutputAs}. */
    public static String gitHubEnvTarget(OutputAs outputAs) {
        return outputAs == OutputAs.pwsh ? "$env:GITHUB_ENV" : "$GITHUB_ENV";
    }

    /** GitHub {@code GITHUB_PATH} target variable for the given {@link OutputAs}. */
    public static String gitHubPathTarget(OutputAs outputAs) {
        return outputAs == OutputAs.pwsh ? "$env:GITHUB_PATH" : "$GITHUB_PATH";
    }
}
