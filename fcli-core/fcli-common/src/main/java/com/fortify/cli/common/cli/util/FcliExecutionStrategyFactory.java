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
package com.fortify.cli.common.cli.util;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionStrategy;

/**
 * Factory for creating the standard fcli execution strategy combining
 * command initialization (CommandSpec & log mask injection, version & args logging)
 * with UnirestContext lifecycle management in a single iteration.
 */
public class FcliExecutionStrategyFactory {
    public static IExecutionStrategy createExecutionStrategy() {
        IExecutionStrategy defaultStrategy = new CommandLine.RunLast();
        return new FcliInitializationExecutionStrategy(defaultStrategy);
    }

    public static CommandLine configureCommandLine(CommandLine cmd) {
        cmd.setExecutionStrategy(createExecutionStrategy());
        return cmd;
    }
}
