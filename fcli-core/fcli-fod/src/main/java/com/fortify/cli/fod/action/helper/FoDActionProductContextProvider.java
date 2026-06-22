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
package com.fortify.cli.fod.action.helper;

import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.action.runner.IActionProductContextProvider;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.rest.helper.FoDUnirestHelper;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;

import kong.unirest.UnirestInstance;

public class FoDActionProductContextProvider implements IActionProductContextProvider {
    @Override
    public String getProductName() {
        return "fod";
    }

    @Override
    public void configureActionContext(ActionRunnerContextLocal ctx, String sessionName) {
        var descriptor = getSessionDescriptor(sessionName);
        ctx.addRequestHelper("fod", new BasicActionRequestHelper(
                () -> createFoDUnirestInstance(descriptor), FoDProductHelper.INSTANCE));
    }

    @Override
    public void configureSpelContext(IConfigurableSpelEvaluator spelEvaluator, ActionRunnerContextLocal ctx, String sessionName) {
        var descriptor = getSessionDescriptor(sessionName);
        spelEvaluator.configure(spelCtx ->
            spelCtx.setVariable("fod", new FoDActionSpelFunctions(
                    () -> ctx.getRequestHelper("fod").getUnirestInstance(),
                    () -> descriptor.getUrlConfig().getUrl(),
                    ctx)));
    }

    private FoDSessionDescriptor getSessionDescriptor(String sessionName) {
        return FoDSessionHelper.instance().get(sessionName, true);
    }

    private static UnirestInstance createFoDUnirestInstance(FoDSessionDescriptor descriptor) {
        return UnirestHelper.createUnirestInstance(
                u -> FoDUnirestHelper.configureUnirestInstance(u, descriptor));
    }
}
