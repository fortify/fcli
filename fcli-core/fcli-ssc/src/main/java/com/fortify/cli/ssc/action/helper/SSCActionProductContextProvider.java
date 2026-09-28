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
package com.fortify.cli.ssc.action.helper;

import java.util.ArrayList;
import java.util.List;

import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallResponseType;
import com.fortify.cli.common.action.runner.ActionRunnerContextLocal;
import com.fortify.cli.common.action.runner.IActionProductContextProvider;
import com.fortify.cli.common.action.runner.processor.IActionRequestHelper.BasicActionRequestHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.UnirestHelper;
import com.fortify.cli.common.spel.IConfigurableSpelEvaluator;
import com.fortify.cli.ssc._common.rest.helper.SSCAndScanCentralUnirestHelper;
import com.fortify.cli.ssc._common.rest.sc_dast.helper.SCDastProductHelper;
import com.fortify.cli.ssc._common.rest.sc_sast.helper.SCSastProductHelper;
import com.fortify.cli.ssc._common.rest.ssc.bulk.SSCBulkRequestBuilder;
import com.fortify.cli.ssc._common.rest.ssc.helper.SSCProductHelper;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionDescriptor;
import com.fortify.cli.ssc._common.session.helper.SSCAndScanCentralSessionHelper;

import kong.unirest.UnirestInstance;

public class SSCActionProductContextProvider implements IActionProductContextProvider {
    @Override
    public String getProductName() {
        return "ssc";
    }

    @Override
    public void configureActionContext(ActionRunnerContextLocal ctx, String sessionName) {
        var descriptor = getSessionDescriptor(sessionName);
        ctx.addRequestHelper("ssc", new SSCActionRequestHelper(
                () -> createSscUnirestInstance(descriptor), SSCProductHelper.INSTANCE));
        ctx.addRequestHelper("sc-sast", new SSCActionRequestHelper(
                () -> createScSastUnirestInstance(descriptor), SCSastProductHelper.INSTANCE));
        ctx.addRequestHelper("sc-dast", new SSCActionRequestHelper(
                () -> createScDastUnirestInstance(descriptor), SCDastProductHelper.INSTANCE));
    }

    @Override
    public void configureSpelContext(IConfigurableSpelEvaluator spelEvaluator, ActionRunnerContextLocal ctx, String sessionName) {
        var descriptor = getSessionDescriptor(sessionName);
        spelEvaluator.configure(spelCtx ->
            spelCtx.setVariable("ssc", new SSCActionSpelFunctions(
                    () -> ctx.getRequestHelper("ssc").getUnirestInstance(),
                    () -> descriptor.getSscUrlConfig().getUrl(),
                    ctx)));
    }

    private SSCAndScanCentralSessionDescriptor getSessionDescriptor(String sessionName) {
        return SSCAndScanCentralSessionHelper.instance().get(sessionName, true);
    }

    private static UnirestInstance createSscUnirestInstance(SSCAndScanCentralSessionDescriptor descriptor) {
        return UnirestHelper.createUnirestInstance(
                u -> SSCAndScanCentralUnirestHelper.configureSscUnirestInstance(u, descriptor));
    }

    private static UnirestInstance createScSastUnirestInstance(SSCAndScanCentralSessionDescriptor descriptor) {
        return UnirestHelper.createUnirestInstance(
                u -> SSCAndScanCentralUnirestHelper.configureScSastControllerUnirestInstance(u, descriptor));
    }

    private static UnirestInstance createScDastUnirestInstance(SSCAndScanCentralSessionDescriptor descriptor) {
        return UnirestHelper.createUnirestInstance(
                u -> SSCAndScanCentralUnirestHelper.configureScDastControllerUnirestInstance(u, descriptor));
    }

    // Package-private for testing
    static final class SSCActionRequestHelper extends BasicActionRequestHelper {
        public SSCActionRequestHelper(IUnirestInstanceSupplier unirestInstanceSupplier, IProductHelper productHelper) {
            super(unirestInstanceSupplier, productHelper);
        }

        @Override
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
            // Bulk responses are JSON-only, so text and file requests are always executed individually
            var bulkableRequests = new ArrayList<ActionRequestDescriptor>();
            for ( var rd : requestDescriptors ) {
                if ( rd.getResponseType()==ActionStepRestCallResponseType.auto ) {
                    bulkableRequests.add(rd);
                } else {
                    executeSingleRequest(getUnirestInstance(), rd);
                }
            }
            if (bulkableRequests.size() == 1) {
                executeSingleRequest(getUnirestInstance(), bulkableRequests.get(0));
            } else if ( !bulkableRequests.isEmpty() ) {
                var bulkRequestBuilder = new SSCBulkRequestBuilder();
                bulkableRequests.forEach(r -> bulkRequestBuilder.request(createRequest(getUnirestInstance(), r), r.getResponseConsumer()));
                bulkRequestBuilder.execute(getUnirestInstance());
            }
        }
    }
}
