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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepWithProduct;
import com.fortify.cli.common.action.runner.ActionProductContextProviders;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorWithProduct extends AbstractActionStepProcessor {
    private final ActionRunnerContextLocal ctx;
    private final ActionStepWithProduct withProduct;

    @Override
    public void process() {
        var provider = ActionProductContextProviders.get(withProduct.getName());
        var sessionExpr = withProduct.getSession();
        var session = sessionExpr != null ? getVars().eval(sessionExpr, String.class) : "default";
        var childCtx = ctx.createChildForProduct(provider, session);
        try {
            new ActionStepProcessorSteps(childCtx, withProduct.get_do()).process();
        } finally {
            childCtx.closeAddedRequestHelpers(ctx);
        }
    }
}
