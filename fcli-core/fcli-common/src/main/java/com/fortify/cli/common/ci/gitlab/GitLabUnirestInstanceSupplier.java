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
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
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
    private static final String API_V4_PATH = "/api/v4";
    @NonNull
    private final UnirestContext unirestContext;
    
    @Builder.Default
    private final IUrlConfig urlConfig = UrlConfig.builder()
        .url(normalizeGitLabUrl(EnvHelper.envOrDefault(GitLabEnvironment.ENV_API_V4_URL, "https://gitlab.com")))
        .build();
    
    @Builder.Default
    private final String token = EnvHelper.env(GitLabEnvironment.ENV_TOKEN);
    
    /**
     * Unique cache key for this supplier instance, ensuring that each instance
     * uses its own dedicated UnirestInstance with the appropriate configuration
     * (base URL, authentication token, etc.). The key is based on the instance's
     * identity hash code to guarantee proper isolation across instances.
     */
    private final String cacheKey = JavaHelper.identity(this);
    
    /**
     * Normalizes a GitLab URL by ensuring it includes the /api/v4 path.
     * If the URL already ends with /api/v4, /api/v3, /api, or contains /api/ in the path,
     * returns the URL unchanged. Otherwise, appends /api/v4.
     * 
     * @param url The base URL to normalize
     * @return Normalized URL with API path
     */
    private static String normalizeGitLabUrl(String url) {
        if (StringUtils.isBlank(url)) {
            return "https://gitlab.com" + API_V4_PATH;
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
        return url + API_V4_PATH;
    }
    
    /**
     * Custom setter for urlConfig that normalizes the URL before setting.
     * This method is called by Lombok's builder when urlConfig is set.
     */
    public static class GitLabUnirestInstanceSupplierBuilder {
        public GitLabUnirestInstanceSupplierBuilder urlConfig(IUrlConfig urlConfig) {
            if (urlConfig != null && urlConfig.getUrl() != null) {
                String normalizedUrl = normalizeGitLabUrl(urlConfig.getUrl());
                this.urlConfig$value = UrlConfig.builderFrom(urlConfig)
                    .url(normalizedUrl)
                    .build();
                this.urlConfig$set = true;
            } else {
                this.urlConfig$value = urlConfig;
                this.urlConfig$set = true;
            }
            return this;
        }
    }
    
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
        return builder(unirestContext).build();
    }
    
    @Override
    public UnirestInstance getUnirestInstance() {
        return unirestContext.getUnirestInstance(cacheKey, this::configureUnirest);
    }
    
    private void configureUnirest(UnirestInstance unirest) {
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        ProxyHelper.configureProxy(unirest, GitLabEnvironment.TYPE, urlConfig.getUrl());
        if (token != null) {
            unirest.config().setDefaultHeader("PRIVATE-TOKEN", token);
        }
    }
}
