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
package com.fortify.cli.fod.action.helper;

import com.fortify.cli.common.action.helper.ActionCommandLineFactory;
import com.fortify.cli.common.action.helper.ActionParameterHelper;
import com.fortify.cli.common.action.helper.ActionParameterHelper.OnUnknownParameterType;
import com.fortify.cli.common.action.helper.ActionParameterHelper.ParameterValueSupplier;
import com.fortify.cli.common.action.model.Action;
import com.fortify.cli.common.action.runner.ActionRunnerCommand;

import lombok.Builder;
import picocli.CommandLine;

@Builder
public final class FoDActionHelper {
    private final Action action;
    private final OnUnknownParameterType onUnknownParameterType;
    
    public final CommandLine createCommandLine() {
        var actionParameterHelper = createActionParameterHelper();
        return ActionCommandLineFactory.builder()
            .action(action)
            .actionParameterHelper(actionParameterHelper)
            .actionRunnerCommand(createActionRunnerCommand(actionParameterHelper))
            .runCmd("fcli fod action run")
            .build()
            .createCommandLine();
    }
    
    private final ActionParameterHelper createActionParameterHelper() {
        return ActionParameterHelper.builder()
            .action(action)
            .onUnknownParameterType(onUnknownParameterType)
            .optionSpecTypeConfigurer("release_single", (b,p)->ParameterValueSupplier.configure(b, null)) //TODO
            .build();
    }
    
    private final ActionRunnerCommand createActionRunnerCommand(ActionParameterHelper actionParameterHelper) {
        return ActionRunnerCommand.builder()
            .action(action)
            .actionParameterHelper(actionParameterHelper)
            .build();
    }

}
