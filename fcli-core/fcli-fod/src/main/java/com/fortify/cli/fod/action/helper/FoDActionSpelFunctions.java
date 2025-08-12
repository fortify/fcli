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
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionParamDescription;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionPrefix;
import com.fortify.cli.common.spring.expression.fn.metadata.annotation.SpelFunctionReturnDescription;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.release.helper.FoDReleaseHelper;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Reflectable
@SpelFunctionPrefix("fod.")
public final class FoDActionSpelFunctions {
    private final FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    private final ActionRunnerContext ctx;
    
    @SpelFunctionDescription("Retrieves the release information based on the provided release name or ID and" 
    		+ " returns it as a JSON `ObjectNode`. Progress updates are written during the operation."
    		+ "  This method uses `FoDReleaseHelper.getReleaseDescriptor` to fetch "
    		+ " release metadata from the Fortify on Demand system. It writes progress messages"
    		+ "	before and after the release is loaded.")
    public final @SpelFunctionReturnDescription("a JSON object containing the release details") ObjectNode release(@SpelFunctionParamDescription("the name or ID of the release to retrieve") String nameOrId) {
        ctx.getProgressWriter().writeProgress("Loading release %s", nameOrId);
        var result = FoDReleaseHelper.getReleaseDescriptor(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":", true);
        ctx.getProgressWriter().writeProgress("Loaded release %s", result.getQualifiedName());
        return result.asObjectNode();
    }

    @SpelFunctionDescription("Generates a browser-accessible deep link URL for a given issue based on its vulnerability ID."
    		+ " This method constructs a redirect URL template for issue tracking and evaluates it"
    		+ " against the provided issue JSON object using Spring Expression Language (SpEL)."
    		+ " The result is a fully resolved URL that can be used to access the issue in the browser.")
    public @SpelFunctionReturnDescription("a browser-accessible URL string pointing to the issue's detail page") String issueBrowserUrl(@SpelFunctionParamDescription("a JSON object representing the issue; expected to contain a field named `vulnId`") ObjectNode issue) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Issues/${vulnId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
    }
    
    @SpelFunctionDescription("Constructs a browser-accessible deep link URL for the release specified in the given application version JSON object."
        		+ " This method builds a redirect URL template using the `releaseId` extracted from the provided"
        		+ " `ObjectNode` and evaluates the template with Spring Expression Language (SpEL). The resulting URL"
        		+ " points to the release detail page within the Fortify on Demand interface.")
    public @SpelFunctionReturnDescription("a fully evaluated URL string for accessing the release in a browser") String releaseBrowserUrl(@SpelFunctionParamDescription("the JSON object representing an application version, expected to contain a `releaseId` field") ObjectNode appversion) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Releases/${releaseId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
    }
    
    @SpelFunctionDescription("Generates a browser-accessible deep link URL for the application specified in the given application version JSON object."
    		+ " This method constructs a redirect URL template using the `applicationId` field from the provided"
    		+ " `ObjectNode`. The template is evaluated using Spring Expression Language (SpEL) to produce the final URL"
    		+ " that points to the application's detail page within the Fortify on Demand interface.")
    public @SpelFunctionReturnDescription("a fully evaluated deep link URL string to access the application details in a browser") String appBrowserUrl(@SpelFunctionParamDescription("a JSON object representing the application version, expected to contain an `applicationId` field") ObjectNode appversion) {
        var deepLinkExpression = baseUrl()
                +"/redirect/Applications/${applicationId}";
        return ctx.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
    }
    private String baseUrl() {
        return FoDProductHelper.INSTANCE.getBrowserUrl(unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl());
    }
}