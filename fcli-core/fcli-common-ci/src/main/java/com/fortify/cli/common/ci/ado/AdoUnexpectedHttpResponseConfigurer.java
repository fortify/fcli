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

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;

import kong.unirest.Config;
import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.Interceptor;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;

/**
 * Configures Azure DevOps-specific HTTP error handling.
 * For environment-based configuration, provides context-aware guidance on authentication
 * and advanced security requirements.
 * For non-environment configuration, delegates to standard error handling.
 * 
 * @author rsenden
 */
public class AdoUnexpectedHttpResponseConfigurer implements Interceptor {
    
    public static void configure(UnirestInstance unirestInstance, String token, boolean configuredFromEnv) {
        if (configuredFromEnv) {
            unirestInstance.config().interceptor(new AdoEnvUnexpectedHttpResponseInterceptor(token));
        } else {
            UnirestUnexpectedHttpResponseConfigurer.configure(unirestInstance);
        }
    }
    
    private static final class AdoEnvUnexpectedHttpResponseInterceptor implements Interceptor {
        private final String token;
        
        private AdoEnvUnexpectedHttpResponseInterceptor(String token) {
            this.token = token;
        }
        
        @Override
        public void onResponse(HttpResponse<?> response, HttpRequestSummary request, Config config) {
            if (!response.isSuccess()) {
                throw new UnexpectedHttpResponseException(response, request, getAuthGuidance(response, request));
            }
        }
        
        @Override
        public HttpResponse<?> onFail(Exception e, HttpRequestSummary request, Config config) throws UnirestException {
            throw (e instanceof UnirestException) ? (UnirestException)e : new UnirestException(e);
        }
        
        private String getAuthGuidance(HttpResponse<?> response, HttpRequestSummary request) {
            int status = response.getStatus();
            String path = request.getUrl();
            
            if (status == 401 || status == 203) {
                return StringUtils.isBlank(token)
                    ? "No ADO_TOKEN found. Set 'ADO_TOKEN: $(System.AccessToken)' in pipeline variables"
                    : "ADO_TOKEN authentication failed. Verify token is valid or refresh System.AccessToken";
            }
            
            if (status == 403) {
                if (path.contains("/advancedsecurity/") || path.contains("/sarif")) {
                    return "Operation requires GitHub Advanced Security for Azure DevOps license and appropriate permissions";
                }
                return "ADO_TOKEN lacks required permissions for this operation";
            }
            
            if (status == 404 && (path.contains("/advancedsecurity/") || path.contains("/sarif"))) {
                return "Advanced Security endpoint not found. Ensure GitHub Advanced Security is enabled for this organization";
            }
            
            return null;
        }
    }
}
