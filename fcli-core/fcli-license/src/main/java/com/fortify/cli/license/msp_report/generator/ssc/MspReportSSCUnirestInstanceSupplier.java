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
package com.fortify.cli.license.msp_report.generator.ssc;

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.UnirestContext;
import com.fortify.cli.common.rest.unirest.config.UnirestJsonHeaderConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.util.JavaHelper;
import com.fortify.cli.license.msp_report.config.MspReportSSCSourceConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;

import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Provides UnirestInstance configuration for SSC REST API operations within MSP reports.
 * Handles base URL, authentication tokens, and SSC-specific headers.
 * 
 * @author rsenden
 */
@Reflectable
@RequiredArgsConstructor
public class MspReportSSCUnirestInstanceSupplier implements IUnirestInstanceSupplier {
    private static final String TYPE = "ssc";
    private final UnirestContext unirestContext;
    private final MspReportSSCSourceConfig sourceConfig;
    
    /**
     * Unique cache key for this supplier instance, ensuring that each instance
     * uses its own dedicated UnirestInstance with the appropriate configuration
     * (base URL, authentication token, etc.). The key is based on the instance's
     * identity hash code to guarantee proper isolation across instances.
     */
    private final String cacheKey = JavaHelper.identity(this);
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return unirestContext.getUnirestInstance(cacheKey, this::configureUnirest);
    }
    
    private void configureUnirest(UnirestInstance unirest) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        
        if (sourceConfig.hasUrlConfig()) {
            UnirestUrlConfigConfigurer.configure(unirest, sourceConfig);
        }
        
        ProxyHelper.configureProxy(unirest, TYPE, sourceConfig.getUrl());
        
        // SSC-specific configuration
        unirest.config().requestCompression(false); // Larger SSC requests fail when compression is enabled
        
        // Configure authentication token
        String tokenExpression = sourceConfig.getTokenExpression();
        if (StringUtils.isBlank(tokenExpression)) {
            throw new FcliSimpleException("SSC configuration requires tokenExpression property");
        }
        
        String token = JsonHelper.evaluateSpelExpression(null, tokenExpression, String.class);
        if (StringUtils.isBlank(token)) {
            throw new FcliSimpleException("No token found from expression: " + tokenExpression);
        }
        
        unirest.config().setDefaultHeader("Authorization", "FortifyToken " + SSCTokenConverter.toRestToken(token));
    }
    
    public void close() {
        unirestContext.close(cacheKey);
    }
}
