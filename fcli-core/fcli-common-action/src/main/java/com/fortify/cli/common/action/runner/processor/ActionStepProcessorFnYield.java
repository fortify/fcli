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
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.action.runner.ActionStepBreakException;
import com.fortify.cli.common.action.runner.FcliActionStepException;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

/**
 * Processor for the {@code fn.yield} step. Evaluates the expression,
 * optionally formats it, and emits the result to the current yield consumer.
 * Throws {@link ActionStepBreakException} if the consumer signals early termination.
 */
@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorFnYield extends AbstractActionStepProcessor {
    private final ActionRunnerContextLocal ctx;
    private final TemplateExpressionWithFormatter fnYield;

    @Override
    public void process() {
        var value = ActionRunnerHelper.formatValueAsJsonNode(ctx, getVars(), fnYield);
        var consumer = ctx.getYieldConsumer();
        if (consumer == null) {
            throw new FcliActionStepException("fn.yield can only be used inside streaming functions");
        }
        if (!consumer.apply(value)) {
            throw new ActionStepBreakException();
        }
    }
}
