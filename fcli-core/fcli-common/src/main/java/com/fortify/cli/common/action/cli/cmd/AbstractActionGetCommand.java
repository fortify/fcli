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

import com.fortify.cli.common.action.helper.ActionHelper;
import com.fortify.cli.common.action.helper.ActionHelper.ActionSignatureHandler;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;

import picocli.CommandLine.Parameters;

public abstract class AbstractActionGetCommand extends AbstractRunnableCommand {
    @Parameters(arity="1", descriptionKey="fcli.action.run.action") private String action;
    
    @Override
    public final Integer call() {
        initMixins();
        System.out.println(ActionHelper.loadActionContents(getType(), action, ActionSignatureHandler.WARN));
        return 0;
    }
    
    protected abstract String getType();
}
