/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.common.action.cli.cmd;

import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.helper.ActionLoaderHelper.ActionValidationHandler;
import com.fortify.cli.common.action.runner.n.ActionCommandLineFactory;
import com.fortify.cli.common.action.runner.n.ActionRuntimeConfig;
import com.fortify.cli.common.action.runner.n.ActionSourceConfig;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;

import picocli.CommandLine;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionNewRunCommand extends AbstractRunnableCommand {
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @Mixin private CommandHelperMixin commandHelper;
    @Unmatched private String[] actionArgs; 

    @Override
    public Integer call() throws Exception {
        initMixins();
        // TODO Handle progress writer, delayed console writers, ...
        createCommandLine().execute(actionArgs==null ? new String[] {} : actionArgs);
        return 0;
    }
    
    private final CommandLine createCommandLine() {
        return ActionCommandLineFactory.builder()
            .actionSourceConfig(new ActionSourceConfig(actionResolver, actionResolver.loadAction(getType(), ActionValidationHandler.WARN)))
            .actionRuntimeConfig(createActionRuntimeConfig())
            .build()
            .createCommandLine();
    }
    
    protected abstract String getType();
    protected abstract ActionRuntimeConfig createActionRuntimeConfig();
}
