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

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.fod._common.session.helper.FoDSessionDescriptor;
import com.fortify.cli.fod._common.session.helper.FoDSessionHelper;

import kong.unirest.core.UnirestInstance;

public class FoDProductHelperBasicMixin extends AbstractSessionUnirestInstanceSupplierMixin<FoDSessionDescriptor> 
    implements IProductHelper
{   
    @Override
    protected final FoDSessionDescriptor getSessionDescriptor(String sessionName) {
        return FoDSessionHelper.instance().get(sessionName, true);
    }

    @Override
    protected final void configure(UnirestInstance unirest, FoDSessionDescriptor sessionDescriptor) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getUrlConfig());
        ProxyHelper.configureProxy(unirest, "fod", sessionDescriptor.getUrlConfig().getUrl());
        final String authHeader = String.format("Bearer %s", sessionDescriptor.getActiveBearerToken());
        unirest.config().setDefaultHeader("Authorization", authHeader);
    }
}