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
package com.fortify.cli.generic_action.action.cli.cmd;

import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.runner.ActionRunnerConfig.ActionRunnerConfigBuilder;

import picocli.CommandLine.Command;

@Command(name = "run")
public class GenericActionRunCommand extends AbstractActionRunCommand {
    @Override
    protected final String getType() {
        return "generic_action";
    }
    
    @Override
    protected void configure(ActionRunnerConfigBuilder actionRunnerConfigBuilder) {
        // Nothing to do
    }
    
}
