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
package com.fortify.cli.common.action.runner.processor;

import java.util.Collections;
import java.util.List;

import com.fortify.cli.common.action.model.ActionStepWith;
import com.fortify.cli.common.action.model.ActionStepWithSession;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.cli.util.FcliCommandExecutorFactory;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class ActionStepWithSessionHandler implements IActionStepWithHandler {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final ActionStepWithSession withSession;
    
    public static final List<? extends IActionStepWithHandler> createHandlers(ActionStepProcessorWith actionStepProcessorWith, ActionRunnerContext ctx, ActionRunnerVars vars, ActionStepWith withStep) {
        var withSessions = withStep.getSessions();
        return withSessions==null || withSessions.isEmpty() 
                ? Collections.emptyList() 
                : withSessions.stream()
                    .filter(actionStepProcessorWith::_if)
                    .map(ws->new ActionStepWithSessionHandler(ctx, vars, ws))
                    .toList();
    }

    @Override
    public final void doBefore() {
        processFcliSessionCmd(withSession.getLoginCommand());
    }
    
    @Override
    public final void doAfter() {
        processFcliSessionCmd(withSession.getLogoutCommand());
    }
    
    @Override
    public final boolean isAddShutdownHandler() {
        return true;
    }
    
    private final void processFcliSessionCmd(TemplateExpression cmd) {
        FcliCommandExecutorFactory.builder()
            .cmd(vars.eval(cmd, String.class))
            .stdout(System.out)
            .stderr(System.err)
            .build().create().execute();
    }
}
