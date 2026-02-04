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
import lombok.NonNull;

/**
 * Provides UnirestInstance configuration for GitLab REST API operations.
 * Handles base URL, authentication tokens, and GitLab-specific headers.
 * Automatically appends /api/v4 path to base URLs that don't already include it.
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public class GitLabUnirestInstanceSupplier implements IUnirestInstanceSupplier {
    @NonNull
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
     * (base URL, authentication token, etc.). The key is based on the instance's
     * identity hash code to guarantee proper isolation across instances.
     */
    private final String cacheKey = JavaHelper.identity(this);
    
    /**
     * Create a builder with required UnirestContext.
     * 
     * @param unirestContext UnirestContext instance (required)
     * @return Builder instance
     */
    public static GitLabUnirestInstanceSupplierBuilder builder(UnirestContext unirestContext) {
        return new GitLabUnirestInstanceSupplierBuilder().unirestContext(unirestContext);
    }
    
    /**
     * Create a supplier using environment-based defaults.
     * 
     * @param unirestContext UnirestContext instance (required)
     * @return Configured supplier instance
     */
    public static GitLabUnirestInstanceSupplier fromEnv(UnirestContext unirestContext) {
        return builder(unirestContext)
            .urlConfig(UrlConfig.builder()
                .url(EnvHelper.envOrDefault(GitLabEnvironment.ENV_API_V4_URL, "https://gitlab.com"))
                .build())
            .token(StringUtils.firstNonBlank(
                EnvHelper.env(GitLabEnvironment.ENV_TOKEN),      // Custom GITLAB_TOKEN (highest priority)
                EnvHelper.env(GitLabEnvironment.ENV_JOB_TOKEN)   // Built-in CI_JOB_TOKEN (automatic fallback)
            ))
            .configuredFromEnv(true)
            .build();
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return unirestContext.getUnirestInstance(cacheKey, this::configureUnirest);
    }
    
    private void configureUnirest(UnirestInstance unirest) {
        GitLabUnexpectedHttpResponseConfigurer.configure(unirest, token, configuredFromEnv);
        UnirestJsonHeaderConfigurer.configure(unirest);
        
        // Normalize URL config to ensure /api/v4 path is present
        IUrlConfig normalizedUrlConfig = normalizeUrlConfig(urlConfig);
        UnirestUrlConfigConfigurer.configure(unirest, normalizedUrlConfig);
        ProxyHelper.configureProxy(unirest, GitLabEnvironment.TYPE, normalizedUrlConfig.getUrl());
        
        if (token != null) {
            unirest.config().setDefaultHeader("PRIVATE-TOKEN", token);
        }
    }
    
    /**
     * Normalizes a URL config by ensuring the URL includes the /api/v4 path.
     * Returns a new UrlConfig if normalization was needed, otherwise returns the original.
     * 
     * @param urlConfig The URL config to normalize
     * @return Normalized URL config
     */
    private static IUrlConfig normalizeUrlConfig(IUrlConfig urlConfig) {
        return urlConfig==null
            ? urlConfig 
            : UrlConfig.builderFrom(urlConfig)
                .url(normalizeUrl(urlConfig.getUrl()))
                .build();
    }
    
    /**
     * Normalizes a GitLab URL by ensuring it includes the /api/v4 path.
     * If the URL already contains /api/, returns it unchanged. Otherwise, appends /api/v4.
     * 
     * @param url The base URL to normalize
     * @return Normalized URL with API path
     */
    private static String normalizeUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "https://gitlab.com/api/v4";
        }
        
        // Remove trailing slashes for consistent checking
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        
        // Check if URL already contains API path
        if (url.contains("/api/")) {
            return url;
        }
        
        // Append /api/v4 to base URL
        return url + "/api/v4";
    }
}
