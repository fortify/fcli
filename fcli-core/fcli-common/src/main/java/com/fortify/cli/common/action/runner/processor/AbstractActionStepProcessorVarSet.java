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
import com.fasterxml.jackson.databind.JsonNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.TemplateExpressionWithFormatter;
import com.fortify.cli.common.action.runner.ActionRunnerHelper;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public abstract class AbstractActionStepProcessorVarSet extends AbstractActionStepProcessorMapEntries<TemplateExpression, TemplateExpressionWithFormatter> {
    @Override
    protected final void process(TemplateExpression key, TemplateExpressionWithFormatter value) {
        setVar(getVarName(key), getVarValue(value));
    }
    
    private final String getVarName(TemplateExpression varNameExpression) {
        return getVars().eval(varNameExpression, String.class);
    }

    private final JsonNode getVarValue(TemplateExpressionWithFormatter templateExpressionWithFormatter) {
        return ActionRunnerHelper.formatValueAsJsonNode(getCtx(), getVars(), templateExpressionWithFormatter);
    }

    protected abstract void setVar(String name, JsonNode value);
}
