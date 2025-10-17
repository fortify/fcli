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

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.model.ActionStepRestTargetEntry;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.ActionRunnerVars;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Data @EqualsAndHashCode(callSuper = true) @Reflectable
public class ActionStepProcessorRestTarget extends AbstractActionStepProcessorMapEntries<String, ActionStepRestTargetEntry> {
    private final ActionRunnerContext ctx;
    private final ActionRunnerVars vars;
    private final LinkedHashMap<String,ActionStepRestTargetEntry> map;
    
    @Override
    protected final void process(String name, ActionStepRestTargetEntry entry) {
        ctx.addRequestHelper(name, createBasicRequestHelper(name, entry));
    }
    
    private IActionRequestHelper createBasicRequestHelper(String name, ActionStepRestTargetEntry entry) {
        var baseUrl = vars.eval(entry.getBaseUrl(), String.class);
        var headers = vars.eval(entry.getHeaders(), String.class);
        UnirestContext context = ctx.getConfig().getUnirestContext();
        IUnirestInstanceSupplier unirestInstanceSupplier = () -> context.getUnirestInstance(name, u->{
            u.config().defaultBaseUrl(baseUrl).getDefaultHeaders().add(headers);
            UnirestUnexpectedHttpResponseConfigurer.configure(u);
            UnirestJsonHeaderConfigurer.configure(u);
        });
        return new BasicActionRequestHelper(unirestInstanceSupplier, null);
    }
}
