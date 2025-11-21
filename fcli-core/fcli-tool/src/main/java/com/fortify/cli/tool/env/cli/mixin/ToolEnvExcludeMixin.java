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
package com.fortify.cli.tool.env.cli.mixin;

import java.util.EnumSet;

import picocli.CommandLine.Option;

/**
 * Shared mixin that controls whether env commands exclude PATH updates, variable
 * exports, or both. Default is to exclude nothing.
 */
public class ToolEnvExcludeMixin {
    @Option(names = {"--excludes"}, split = ",", descriptionKey = "fcli.tool.env.exclude")
    private EnumSet<OutputComponent> exclude = EnumSet.noneOf(OutputComponent.class);

    public boolean isIncludePath() {
        return !exclude.contains(OutputComponent.path);
    }

    public boolean isIncludeVars() {
        return !exclude.contains(OutputComponent.vars);
    }

    public enum OutputComponent {
        path,
        vars
    }
}
