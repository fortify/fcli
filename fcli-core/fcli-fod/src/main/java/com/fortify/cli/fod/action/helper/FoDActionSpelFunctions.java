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
package com.fortify.cli.fod.action.helper;

import static com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction.SpelFunctionCategory.fortify;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.spel.SpelHelper;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunction;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionParam;
import com.fortify.cli.common.spel.fn.descriptor.annotation.SpelFunctionPrefix;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Reflectable
@SpelFunctionPrefix("fod.")
public final class FoDActionSpelFunctions {
    private final FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    private final ActionRunnerContext ctx;
    
    @SpelFunction(cat=fortify, returns="FoD release object for the given release name or id") 
    public final ObjectNode release(
            @SpelFunctionParam(name="nameOrId", desc="the name or ID of the release to load") String nameOrId)
    {
        ctx.getProgressWriter().writeProgress("Loading release %s", nameOrId);
        var result = FoDReleaseHelper.getReleaseDescriptor(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":", true);
        ctx.getProgressWriter().writeProgress("Loaded release %s", result.getQualifiedName());
        return result.asObjectNode();
    }

    @SpelFunction(cat=fortify, returns="Browser-accessible URL pointing to the FoD issue details page for the given issue")
    public String issueBrowserUrl(
            @SpelFunctionParam(name="issue", desc="an FoD issue object, containing at least the `vulnId` field") ObjectNode issue)
    {
        var deepLinkExpression = baseUrl()
                +"/redirect/Issues/${vulnId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
    }
    
    @SpelFunction(cat=fortify, returns="Browser-accessible URL pointing to the FoD release page for the given release") 
    public String releaseBrowserUrl(
            @SpelFunctionParam(name="rel", desc="an FoD release object, for example as returned by `#fod.release(...)`, containing at least the `releaseId` field") ObjectNode release)
    {
        var deepLinkExpression = baseUrl()
                +"/redirect/Releases/${releaseId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), release, String.class);
    }
    
    @SpelFunction(cat=fortify, returns="Browser-accessible URL pointing to the FoD application page for the given release") 
    public String appBrowserUrl(
            @SpelFunctionParam(name="rel", desc="an FoD release object, for example as returned by `#fod.release(...)`, containing at least the `applicationId` field") ObjectNode release)
    {
        var deepLinkExpression = baseUrl()
                +"/redirect/Applications/${applicationId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), release, String.class);
    }
    private String baseUrl() {
        return FoDProductHelper.INSTANCE.getBrowserUrl(unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl());
    }
}