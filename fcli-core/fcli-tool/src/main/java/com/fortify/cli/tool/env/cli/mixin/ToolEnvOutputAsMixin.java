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
package com.fortify.cli.tool.env.cli.mixin;

import picocli.CommandLine.Option;

/**
 * Optional mixin for CI-specific env commands (ado, github, gitlab) that
 * allows the caller to request additional shell or PowerShell assignment
 * output so the generated script can both signal the CI system and update
 * the current task's environment in a single pipe.
 */
public class ToolEnvOutputAsMixin {
    public enum OutputAs { shell, pwsh }

    @Option(names = "--output-as", descriptionKey = "fcli.tool.env.output-as")
    private OutputAs outputAs;

    public OutputAs getOutputAs() { return outputAs; }
    public boolean hasOutputAs() { return outputAs != null; }
}
