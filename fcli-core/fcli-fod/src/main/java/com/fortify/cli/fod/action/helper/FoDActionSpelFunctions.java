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
package com.fortify.cli.fod.action.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Reflectable
public final class FoDActionSpelFunctions {
    private final FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    private final ActionRunnerContext ctx;
    
    public final ObjectNode release(String nameOrId) {
        ctx.getProgressWriter().writeProgress("Loading release %s", nameOrId);
        var result = FoDReleaseHelper.getReleaseDescriptor(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":", true);
        ctx.getProgressWriter().writeProgress("Loaded release %s", result.getQualifiedName());
        return result.asObjectNode();
    }
    public String issueBrowserUrl(ObjectNode issue) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Issues/${vulnId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
    }
    public String releaseBrowserUrl(ObjectNode appversion) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Releases/${releaseId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
    }
    public String appBrowserUrl(ObjectNode appversion) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Applications/${applicationId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
    }
    private String baseUrl() {
        return FoDProductHelper.INSTANCE.getBrowserUrl(unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl());
    }
}