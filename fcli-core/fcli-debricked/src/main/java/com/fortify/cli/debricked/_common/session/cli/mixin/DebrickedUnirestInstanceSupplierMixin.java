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
package com.fortify.cli.debricked._common.session.cli.mixin;

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.GenericUnirestFactory;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.session.cli.mixin.AbstractSessionDescriptorSupplierMixin;
import com.fortify.cli.debricked._common.session.helper.DebrickedAuthHelper;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionDescriptor;
import com.fortify.cli.debricked._common.session.helper.DebrickedSessionHelper;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;

public class DebrickedUnirestInstanceSupplierMixin extends AbstractSessionDescriptorSupplierMixin<DebrickedSessionDescriptor> implements IUnirestInstanceSupplier {
    @Getter @ArgGroup(headingKey = "debricked.session.name.arggroup") 
    private DebrickedSessionNameArgGroup sessionNameSupplier;
    
    @Override
    protected final DebrickedSessionDescriptor getSessionDescriptor(String sessionName) {
        var descriptor = DebrickedSessionHelper.instance().get(sessionName, true);
        if ( !descriptor.hasActiveJwtToken() ) {
            descriptor = refreshJwtToken(sessionName, descriptor);
        }
        return descriptor;
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        DebrickedSessionDescriptor sessionDescriptor = getSessionDescriptor();
        String key = "debricked/" + getSessionName();
        return GenericUnirestFactory.getUnirestInstance(key, 
                u -> configure(u, sessionDescriptor));
    }
    
    public static final void shutdownUnirestInstance(String sessionName) {
        GenericUnirestFactory.shutdown("debricked/" + sessionName);
    }
    
    protected final void configure(UnirestInstance unirest, DebrickedSessionDescriptor sessionDescriptor) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, sessionDescriptor.getUrlConfig());
        ProxyHelper.configureProxy(unirest, "debricked", sessionDescriptor.getUrlConfig().getUrl());
        
        String jwtToken = sessionDescriptor.getActiveJwtToken();
        if (jwtToken != null) {
            String authHeader = String.format("Bearer %s", jwtToken);
            unirest.config().setDefaultHeader("Authorization", authHeader);
            UnirestJsonHeaderConfigurer.configure(unirest);
        }
    }
    
    private static final DebrickedSessionDescriptor refreshJwtToken(String sessionName, DebrickedSessionDescriptor descriptor) {
        var urlConfig = descriptor.getUrlConfig();
        var refreshToken = descriptor.getRefreshToken();
        try ( var unirest = GenericUnirestFactory.createUnirestInstance() ) {
            var jwtTokenResponse = DebrickedAuthHelper.getJwtTokenResponse(unirest, urlConfig, refreshToken);
            descriptor = new DebrickedSessionDescriptor(descriptor.getUrlConfig(), jwtTokenResponse.getToken(), jwtTokenResponse.getRefreshToken());
            DebrickedSessionHelper.instance().save(sessionName, descriptor);
        }
        return descriptor;
    }
}