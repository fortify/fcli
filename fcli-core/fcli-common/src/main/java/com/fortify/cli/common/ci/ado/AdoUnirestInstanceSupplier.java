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
package com.fortify.cli.common.ci.ado;

import java.util.Base64;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.UnirestInstance;
import lombok.Builder;

/**
 * Provides UnirestInstance configuration for Azure DevOps REST API operations.
 * Handles base URL (including organization/collection), authentication tokens, 
 * proxy configuration, and other ADO-specific settings.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public class AdoUnirestInstanceSupplier implements IUnirestInstanceSupplier {
    private final UnirestContext unirestContext;
    private final IUrlConfig urlConfig;
    private final String token;
    
    /**
     * Indicates whether this supplier was configured from environment variables.
     * When true, error messages will include environment-specific guidance.
     */
    @Builder.Default
    private final boolean configuredFromEnv = false;
    
    /**
     * Unique cache key for this supplier instance, ensuring that each instance
     * uses its own dedicated UnirestInstance with the appropriate configuration
     * (base URL, organization, authentication token, etc.). The key is based on 
     * the instance's identity hash code to guarantee proper isolation across instances.
     */
    private final String cacheKey = JavaHelper.identity(this);
    
    /**
     * Create a builder with required UnirestContext.
     * 
     * @param unirestContext UnirestContext instance (required)
     * @return Builder instance
     */
    public static AdoUnirestInstanceSupplierBuilder builder(UnirestContext unirestContext) {
        return new AdoUnirestInstanceSupplierBuilder().unirestContext(unirestContext);
    }
    
    /**
     * Create a supplier using environment-based defaults.
     * 
     * @param unirestContext UnirestContext instance (required)
     * @return Configured supplier instance
     */
    public static AdoUnirestInstanceSupplier fromEnv(UnirestContext unirestContext) {
        return builder(unirestContext)
            .urlConfig(UrlConfig.builder()
                .url(EnvHelper.env(AdoEnvironment.ENV_ORGANIZATION_URL))
                .build())
            .token(EnvHelper.env(AdoEnvironment.ENV_TOKEN))
            .configuredFromEnv(true)
            .build();
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return unirestContext.getUnirestInstance(cacheKey, this::configureUnirest);
    }
    
    private void configureUnirest(UnirestInstance unirest) {
        AdoUnexpectedHttpResponseConfigurer.configure(unirest, token, configuredFromEnv);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, AdoEnvironment.TYPE, urlConfig.getUrl());
        if (token != null) {
            String auth = Base64.getEncoder().encodeToString((":" + token).getBytes());
            unirest.config().setDefaultHeader("Authorization", "Basic " + auth);
        }
    }
}
