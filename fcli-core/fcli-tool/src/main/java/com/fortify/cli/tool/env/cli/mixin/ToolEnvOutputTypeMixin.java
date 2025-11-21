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
 * Shared mixin that controls whether env commands emit PATH updates, variable
 * exports, or both. Default is to emit both.
 */
public class ToolEnvOutputTypeMixin {
    @Option(names = {"-o", "--outputs"}, split = ",", defaultValue = "path,vars", descriptionKey = "fcli.tool.env.output-type")
    private EnumSet<OutputComponent> outputType = EnumSet.of(OutputComponent.path, OutputComponent.vars);

    public boolean isIncludePath() {
        return outputType.contains(OutputComponent.path);
    }

    public boolean isIncludeVars() {
        return outputType.contains(OutputComponent.vars);
    }

    public enum OutputComponent {
        path,
        vars
    }
}
