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
package com.fortify.cli.common.action.runner.processor;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.action.model.ActionStepWith;
import com.fortify.cli.common.action.model.ActionStepWithCleanup;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data
public class ActionStepWithCleanupHandler implements IActionStepWithHandler {
    private final ActionRunnerContextLocal ctx;
    private final ActionStepWithCleanup withCleanup;
    
    public static final List<? extends IActionStepWithHandler> createHandlers(ActionStepProcessorWith actionStepProcessorWith, ActionRunnerContextLocal ctx, ActionStepWith withStep) {
        List<ActionStepWithCleanupHandler> result = new ArrayList<>();
        var withCleanup = withStep.getCleanup();
        if ( withCleanup!=null && actionStepProcessorWith._if(withCleanup) ) {
            result.add(new ActionStepWithCleanupHandler(ctx, withCleanup));
        }
        return result;
    }

    @Override
    public final void doBefore() {
        new ActionStepProcessorSteps(ctx, withCleanup.getInitSteps()).process();
    }
    
    @Override
    public final void doAfter() {
        new ActionStepProcessorSteps(ctx, withCleanup.getCleanupSteps()).process();
    }
    
    @Override
    public final boolean isAddShutdownHandler() {
        // TOOD Make this configurable
        return true;
    }
}
