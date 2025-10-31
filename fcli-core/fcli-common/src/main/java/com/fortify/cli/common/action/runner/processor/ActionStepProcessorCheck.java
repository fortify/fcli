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
package com.fortify.cli.common.action.runner.processor;
import java.util.LinkedHashMap;

import com.fasterxml.jackson.databind.node.TextNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepCheckEntry;
import com.fortify.cli.common.action.model.ActionStepCheckEntry.CheckStatus;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorCheck extends AbstractActionStepProcessorMapEntries<String, ActionStepCheckEntry> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<String,ActionStepCheckEntry> map;
    
    @Override
    protected final void process(String key, ActionStepCheckEntry checkStep) {
        var failIf = checkStep.getFailIf();
        var passIf = checkStep.getPassIf();
        var pass = passIf!=null 
                ? vars.eval(passIf, Boolean.class)
                : !vars.eval(failIf, Boolean.class);
        var currentStatus = pass ? CheckStatus.PASS : CheckStatus.FAIL;
        var newCheckStatus = ctx.getCheckStatuses().compute(checkStep, (s,oldStatus)->CheckStatus.combine(oldStatus, currentStatus));
        vars.set("checkStatus."+key, new TextNode(newCheckStatus.name()));
    }
}
