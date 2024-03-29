/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.action.cli.cmd;

import com.fortify.cli.common.action.helper.ActionDescriptor;
import com.fortify.cli.common.action.helper.ActionHelper;
import com.fortify.cli.common.action.helper.ActionParameterHelper;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;

import picocli.CommandLine.Parameters;
import picocli.CommandLine.Unmatched;

public abstract class AbstractActionHelpCommand extends AbstractRunnableCommand implements Runnable {
    @Parameters(arity="1", descriptionKey="fcli.action.run.action") private String action;
    @Unmatched private String[] actionArgs; // We explicitly ignore any unknown CLI args, to allow for 
                                            // users to simply switch between run and help commands.
    
    @Override
    public final void run() {
        initMixins();
        var actionDescriptor = ActionHelper.load(getType(), action);
        System.out.println(getActionHelp(actionDescriptor));
    }
    
    private final String getActionHelp(ActionDescriptor action) {
        var usage = action.getUsage();
        return String.format(
            "\nAction: %s\n"+
            "\n%s\n"+
            "\n%s\n"+
            "\nAction options:\n"+
            "%s",
            action.getName(), usage.getHeader(), usage.getDescription(), ActionParameterHelper.getSupportedOptionsTable(action));
    }
    
    protected abstract String getType();
}
