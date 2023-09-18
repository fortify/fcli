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
package com.fortify.cli.fod._common.output.mixin;

import org.apache.http.impl.client.HttpClientBuilder;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.fod._common.rest.helper.FoDRateLimitRetryStrategy;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;

import kong.unirest.Config;
import kong.unirest.UnirestInstance;
import kong.unirest.apache.ApacheClient;

public class FoDProductHelperBasicMixin extends AbstractSessionUnirestInstanceSupplierMixin<FoDSessionDescriptor> 
    implements IProductHelper
{   
    @Override
    protected final FoDSessionDescriptor getSessionDescriptor(String sessionName) {
        return FoDSessionHelper.instance().get(sessionName, true);
    }

    @Override
    protected final void configure(UnirestInstance unirest, FoDSessionDescriptor sessionDescriptor) {
        // Ideally, we should be able to use unirest::config::retryAfter to handle FoD rate limits,
        // but this is not possible for various reasons (see https://github.com/Kong/unirest-java/issues/491).
        // As such, we use a custom ApacheClient with custom ServiceUnavailableRetryStrategy to handle
        // rate-limited requests. Note that newer Unirest versions are no longer based on Apache HttpClient,
        // so we'll likely need to find an alternative approach if we ever wish to upgrade to Unirest 4.x.
        unirest.config().httpClient(this::createClient);
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getUrlConfig());
        ProxyHelper.configureProxy(unirest, "fod", sessionDescriptor.getUrlConfig().getUrl());
        final String authHeader = String.format("Bearer %s", sessionDescriptor.getActiveBearerToken());
        unirest.config().setDefaultHeader("Authorization", authHeader);
    }
    
    private ApacheClient createClient(Config config) {
        return new ApacheClient(config, this::configureClient);
    }
    
    private void configureClient(HttpClientBuilder cb) {
        cb.setServiceUnavailableRetryStrategy(new FoDRateLimitRetryStrategy());
    }
}