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
package com.fortify.cli.ssc.action.cli.cmd;

import java.util.List;

import org.springframework.expression.spel.support.SimpleEvaluationContext;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.action.cli.cmd.AbstractActionRunCommand;
import com.fortify.cli.common.action.runner.ActionRunnerConfig.ActionRunnerConfigBuilder;
import com.fortify.cli.common.action.runner.ActionRunnerContext;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.ssc._common.rest.cli.mixin.SSCAndScanCentralUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc._common.rest.sc_dast.helper.SCDastProductHelper;
import com.fortify.cli.ssc._common.rest.sc_sast.helper.SCSastProductHelper;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.ssc.helper.SSCProductHelper;
import com.fortify.cli.ssc.action.helper.SSCActionSpelFunctions;

import kong.unirest.HttpRequest;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = "run")
public class SSCActionRunCommand extends AbstractActionRunCommand {
    @Mixin private SSCAndScanCentralUnirestInstanceSupplierMixin unirestInstanceSupplier;
    
    @Override
    protected final String getType() {
        return "SSC";
    }
    
    @Override
    protected void configure(ActionRunnerConfigBuilder configBuilder) {
    configBuilder
            .defaultFcliRunOption("--ssc-session", unirestInstanceSupplier.getSessionName())
            .actionContextConfigurer(this::configureActionContext)
            .actionContextSpelEvaluatorConfigurer(this::configureSpelContext);
    }
    
    protected void configureActionContext(ActionRunnerContext ctx) {
        ctx.addRequestHelper("ssc", new SSCActionRequestHelper(unirestInstanceSupplier::getSscUnirestInstance, SSCProductHelper.INSTANCE));
        ctx.addRequestHelper("sc-sast", new SSCActionRequestHelper(unirestInstanceSupplier::getScSastUnirestInstance, SCSastProductHelper.INSTANCE));
        ctx.addRequestHelper("sc-dast", new SSCActionRequestHelper(unirestInstanceSupplier::getScDastUnirestInstance, SCDastProductHelper.INSTANCE));
    }
    
    protected void configureSpelContext(ActionRunnerContext actionRunnerContext, SimpleEvaluationContext spelContext) {
        spelContext.setVariable("ssc", new SSCActionSpelFunctions(unirestInstanceSupplier, actionRunnerContext));   
    }
    
    private static final class SSCActionRequestHelper extends BasicActionRequestHelper {
        public SSCActionRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
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
            var request = getUnirestInstance().request(requestDescriptor.getMethod(), requestDescriptor.getUri())
                    .queryString(requestDescriptor.getQueryParams());
            var body = requestDescriptor.getBody();
            return body==null ? request : request.body(body);
        }
    }
}
