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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.spel.wrapper.TemplateExpression;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * This abstract base class provides utility methods for removing action variables.
 */
@NoArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public abstract class AbstractActionStepProcessorVarRm extends AbstractActionStepProcessorListEntries<TemplateExpression> {
    @Override
    protected final void process(TemplateExpression entry) {
        rmVar(getVars().eval(entry, String.class));
    }
    
    protected abstract void rmVar(String name);
}
