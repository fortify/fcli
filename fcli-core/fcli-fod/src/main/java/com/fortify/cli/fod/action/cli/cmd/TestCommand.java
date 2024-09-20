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
package com.fortify.cli.fod.action.cli.cmd;

import com.fortify.cli.common.action.cli.mixin.ActionResolverMixin;
import com.fortify.cli.common.action.cli.mixin.ActionValidationMixin;
import com.fortify.cli.common.action.helper.ActionParameterHelper.OnUnknownParameterType;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.cli.mixin.CommandHelperMixin;
import com.fortify.cli.fod.action.helper.FoDActionHelper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Unmatched;

@Command(name = "test")
public class TestCommand extends AbstractRunnableCommand {
    @Mixin private ActionResolverMixin.RequiredParameter actionResolver;
    @Mixin private CommandHelperMixin commandHelper;
    @Mixin private ActionValidationMixin actionValidationMixin;
    @Unmatched private String[] actionArgs = new String[] {};

    @Override
    public Integer call() throws Exception {
        var validationHandler = actionValidationMixin.getActionValidationHandler();
        var action = actionResolver.load("FoD", validationHandler).getAction();
        return FoDActionHelper.builder()
                .action(action)
                .onUnknownParameterType(OnUnknownParameterType.WARN)
                .build()
                .createCommandLine()
                .execute(actionArgs);
    }
    

}
