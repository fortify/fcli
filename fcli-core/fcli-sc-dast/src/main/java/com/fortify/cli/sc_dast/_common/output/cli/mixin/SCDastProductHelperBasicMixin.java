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
package com.fortify.cli.sc_dast._common.output.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionUnirestInstanceSupplierMixin;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionDescriptor;
import com.fortify.cli.sc_dast._common.session.helper.SCDastSessionHelper;

import kong.unirest.core.UnirestInstance;

public class SCDastProductHelperBasicMixin extends AbstractSessionUnirestInstanceSupplierMixin<SCDastSessionDescriptor> 
    implements IProductHelper
{
    @Override
    protected final void configure(UnirestInstance unirest, SCDastSessionDescriptor sessionDescriptor) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getScDastUrlConfig());
        ProxyHelper.configureProxy(unirest, "sc-dast", sessionDescriptor.getScDastUrlConfig().getUrl());
        unirest.config().requestCompression(false); // TODO Check whether SC DAST suffers from the same issue as SSC, with some requests failing if compression is enabled
        unirest.config().setDefaultHeader("Authorization", "FortifyToken "+new String(sessionDescriptor.getActiveToken()));
    }
    
    @Override
    protected final SCDastSessionDescriptor getSessionDescriptor(String sessionName) {
        return SCDastSessionHelper.instance().get(sessionName, true);
    }
}