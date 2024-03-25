/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.ssc.action.cli.cmd;

import java.util.List;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.helper.ActionDescriptor.ActionValidationException;
import com.fortify.cli.common.action.helper.ActionRunner;
import com.fortify.cli.common.action.helper.ActionRunner.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.action.helper.ActionRunner.ParameterTypeConverterArgs;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.spring.expression.SpelHelper;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.ssc._common.rest.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.helper.SSCProductHelper;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.appversion.helper.SSCAppVersionHelper;
import com.fortify.cli.ssc.issue.helper.SSCIssueFilterSetHelper;

import kong.unirest.HttpRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "run")
public class SSCActionRunCommand extends AbstractActionRunCommand {
    @Getter @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    @Override
    protected final String getType() {
        return "SSC";
    }
    
    @Override
    protected void configure(ActionRunner templateRunner, SimpleEvaluationContext context) {
        templateRunner
            .addParameterConverter("appversion_single", this::loadAppVersion)
            .addParameterConverter("filterset", this::loadFilterSet)
            .addRequestHelper("ssc", new SSCDataExtractRequestHelper(unirestInstanceSupplier::getUnirestInstance, SSCProductHelper.INSTANCE));
        context.setVariable("ssc", new SSCSpelFunctions(templateRunner));
    }
    
    @RequiredArgsConstructor @Reflectable
    public final class SSCSpelFunctions {
        private final ActionRunner templateRunner;
        public String issueBrowserUrl(ObjectNode issue, ObjectNode filterset) {
            var deepLinkExpression = baseUrl()
                    +"/html/ssc/version/${projectVersionId}/fix/${id}/?engineType=${engineType}&issue=${issueInstanceId}";
            if ( filterset!=null ) { 
                deepLinkExpression+="&filterSet="+filterset.get("guid").asText();
            }
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), issue, String.class);
        }
        public String appversionBrowserUrl(ObjectNode appversion) {
            var deepLinkExpression = baseUrl()
                    +"/html/ssc/index.jsp#!/version/${id}/fix";
            return templateRunner.getSpelEvaluator().evaluate(SpelHelper.parseTemplateExpression(deepLinkExpression), appversion, String.class);
        }
        private String baseUrl() {
            return unirestInstanceSupplier.getSessionDescriptor().getUrlConfig().getUrl()
                    .replaceAll("/+$", "");
        }
        
    }
    
    private final JsonNode loadAppVersion(String nameOrId, ParameterTypeConverterArgs args) {
        args.getProgressWriter().writeProgress("Loading application version %s", nameOrId);
        var result = SSCAppVersionHelper.getRequiredAppVersion(unirestInstanceSupplier.getUnirestInstance(), nameOrId, ":");
        args.getProgressWriter().writeProgress("Loaded application version %s", result.getAppAndVersionName());
        return result.asJsonNode();
    }
    
    private final JsonNode loadFilterSet(String titleOrId, ParameterTypeConverterArgs args) {
        var progressMessage = StringUtils.isBlank(titleOrId) 
                ? "Loading default filter set" 
                : String.format("Loading filter set %s", titleOrId);
        args.getProgressWriter().writeProgress(progressMessage);
        var parameter = args.getParameter();
        var typeParameters = parameter.getTypeParameters();
        var appVersionIdExpression = typeParameters==null ? null : typeParameters.get("appversion.id");
        if ( appVersionIdExpression==null ) {
            appVersionIdExpression = SpelHelper.parseTemplateExpression("${appversion?.id}");
        }
        var appVersionId = args.getSpelEvaluator().evaluate(appVersionIdExpression, args.getParameters(), String.class);
        if ( StringUtils.isBlank(appVersionId) ) {
            throw new ActionValidationException(String.format("Template parameter %s requires ${%s} to be available", parameter.getName(), appVersionIdExpression.getExpressionString()));
        }
        var filterSetDescriptor = new SSCIssueFilterSetHelper(unirestInstanceSupplier.getUnirestInstance(), appVersionId).getDescriptorByTitleOrId(titleOrId, false);
        if ( filterSetDescriptor==null ) {
            throw new IllegalArgumentException("Unknown filter set: "+titleOrId);
        }
        return filterSetDescriptor.asJsonNode();
    }
    
    private static final class SSCDataExtractRequestHelper extends BasicActionRequestHelper {
        public SSCDataExtractRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
            super(unirestInstanceSupplier, productHelper);
        }

        @Override
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
            if ( requestDescriptors.size()==1 ) {
                var rd = requestDescriptors.get(0);
                createRequest(rd).asObject(JsonNode.class).ifSuccess(r->rd.getResponseConsumer().accept(r.getBody()));
            } else {
                var bulkRequestBuilder = new SSCBulkRequestBuilder();
                requestDescriptors.forEach(r->bulkRequestBuilder.request(createRequest(r), r.getResponseConsumer()));
                bulkRequestBuilder.execute(getUnirestInstance());
            }
        }
        
        private HttpRequest<?> createRequest(ActionRequestDescriptor requestDescriptor) {
            var request = getUnirestInstance(). request(requestDescriptor.getMethod(), requestDescriptor.getUri())
                    .queryString(requestDescriptor.getQueryParams());
            var body = requestDescriptor.getBody();
            return body==null ? request : request.body(body);
        }
    }
}
