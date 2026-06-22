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
package com.fortify.cli.common.ci.bitbucket;

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
 * Configures Bitbucket-specific HTTP error handling.
 * For environment-based configuration, provides context-aware guidance on authentication
 * and proxy-based auth.
 * For non-environment configuration, delegates to standard error handling.
 * 
 * @author rsenden
 */
public class BitbucketUnexpectedHttpResponseConfigurer implements Interceptor {
    
    public static void configure(UnirestInstance unirestInstance, String token, boolean usingProxy, boolean configuredFromEnv) {
        if (configuredFromEnv) {
            unirestInstance.config().interceptor(new BitbucketEnvUnexpectedHttpResponseInterceptor(token, usingProxy));
        } else {
            UnirestUnexpectedHttpResponseConfigurer.configure(unirestInstance);
        }
    }
    
    private static final class BitbucketEnvUnexpectedHttpResponseInterceptor implements Interceptor {
        private final String token;
        private final boolean usingProxy;
        
        private BitbucketEnvUnexpectedHttpResponseInterceptor(String token, boolean usingProxy) {
            this.token = token;
            this.usingProxy = usingProxy;
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
            
            if (status == 401) {
                if (usingProxy) {
                    return "Bitbucket Pipelines proxy authentication failed. Ensure running in Bitbucket Pipelines environment";
                }
                if (StringUtils.isBlank(token)) {
                    return "No authentication found. Set BITBUCKET_TOKEN or BITBUCKET_USERNAME/BITBUCKET_APP_PASSWORD";
                }
                return "Authentication failed. Verify token or app password is valid";
            }
            
            if (status == 403) {
                if (path.contains("/reports/")) {
                    return usingProxy
                        ? "Bitbucket Pipelines proxy lacks permissions. Ensure pipeline has appropriate repository access"
                        : "Token lacks 'repository:write' scope for Code Insights";
                }
                return "Token lacks required permissions for this operation";
            }
            
            if (status == 404 && path.contains("/reports/")) {
                return "Code Insights endpoint not found. Verify workspace, repository, and commit are correct";
            }
            
            return null;
        }
    }
}
