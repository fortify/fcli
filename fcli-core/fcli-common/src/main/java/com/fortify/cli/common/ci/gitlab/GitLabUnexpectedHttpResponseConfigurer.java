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
package com.fortify.cli.common.ci.gitlab;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.util.EnvHelper;

import kong.unirest.Config;
import kong.unirest.HttpRequestSummary;
import kong.unirest.HttpResponse;
import kong.unirest.Interceptor;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;

/**
 * Configures GitLab-specific HTTP error handling.
 * For environment-based configuration, provides context-aware guidance on authentication,
 * job token configuration, and tier requirements.
 * For non-environment configuration, delegates to standard error handling.
 * 
 * @author rsenden
 */
public class GitLabUnexpectedHttpResponseConfigurer implements Interceptor {
    
    public static void configure(UnirestInstance unirestInstance, String token, boolean configuredFromEnv) {
        if (configuredFromEnv) {
            unirestInstance.config().interceptor(new GitLabEnvUnexpectedHttpResponseInterceptor(token));
        } else {
            UnirestUnexpectedHttpResponseConfigurer.configure(unirestInstance);
        }
    }
    
    private static final class GitLabEnvUnexpectedHttpResponseInterceptor implements Interceptor {
        private final String token;
        private final boolean usingJobToken;
        
        private GitLabEnvUnexpectedHttpResponseInterceptor(String token) {
            this.token = token;
            this.usingJobToken = token != null && token.equals(EnvHelper.env(GitLabEnvironment.ENV_JOB_TOKEN));
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
                if (StringUtils.isBlank(token)) {
                    return "No authentication token found. Set GITLAB_TOKEN environment variable or configure CI_JOB_TOKEN access";
                }
                return usingJobToken
                    ? "CI_JOB_TOKEN authentication failed. Enable job token access in project settings or use GITLAB_TOKEN"
                    : "GITLAB_TOKEN authentication failed. Verify token is valid and has 'api' scope";
            }
            
            if (status == 403) {
                if (path.contains("/security_report")) {
                    return usingJobToken
                        ? "CI_JOB_TOKEN lacks permissions. Configure job token scope in project CI/CD settings or use GITLAB_TOKEN with 'api' scope"
                        : "GITLAB_TOKEN lacks required permissions. Ensure token has 'api' scope";
                }
                return usingJobToken
                    ? "CI_JOB_TOKEN lacks permissions for this operation. Use GITLAB_TOKEN with appropriate scope"
                    : "GITLAB_TOKEN lacks required permissions. Verify token scope";
            }
            
            if (status == 404 && path.contains("/security_report")) {
                return "Security Dashboard not available. Requires GitLab Ultimate/Premium tier";
            }
            
            return null;
        }
    }
}
