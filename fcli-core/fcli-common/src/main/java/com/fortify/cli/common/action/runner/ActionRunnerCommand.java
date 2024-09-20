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
package com.fortify.cli.common.action.runner;

import java.util.concurrent.Callable;

import com.fortify.cli.common.action.helper.ActionParameterHelper;
import com.fortify.cli.common.action.model.Action;

import lombok.Builder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command
@Builder
public class ActionRunnerCommand implements Callable<Integer> {
    /** Action to run, provided through builder method */
    private final Action action;
    /** ActionParameterHelper instance, configured through builder method */
    private final ActionParameterHelper actionParameterHelper;
    @Spec private CommandSpec spec;

    public Integer call() {
        System.out.println(spec);
        System.out.println(actionParameterHelper.getParameterValues(spec, null));
        return 0;
    }
}
