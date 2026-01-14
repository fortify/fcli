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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

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
import com.fortify.cli.common.util.FcliDockerHelper;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.UnirestInstance;
import lombok.Builder;

/**
 * Supplies UnirestInstance instances configured for Bitbucket REST API calls,
 * handling base URL overrides, proxy settings, and authentication.
 * 
 * <p>Authentication Methods (checked in order):
 * <ol>
 * <li><b>Bearer Token</b>: {@code BITBUCKET_STEP_OAUTH_ACCESS_TOKEN} or {@code BITBUCKET_TOKEN}</li>
 * <li><b>Basic Auth</b>: {@code BITBUCKET_USERNAME} + {@code BITBUCKET_APP_PASSWORD}</li>
 * <li><b>Bitbucket Pipelines Proxy</b> (automatic when running in Bitbucket Pipelines without credentials):
 *     Uses localhost:29418 proxy (or host.docker.internal:29418 in Docker containers/pipes) which
 *     automatically adds authentication headers for the Reports API without requiring explicit credentials.</li>
 * </ol>
 * 
 * @author rsenden
 */
@Reflectable
@Builder
public class BitbucketUnirestInstanceSupplier implements IUnirestInstanceSupplier {
    private final UnirestContext unirestContext;
    private final IUrlConfig urlConfig;
    private final String oauthToken;
    private final String token;
    private final String username;
    private final String appPassword;
    
    /**
     * Indicates whether this supplier was configured from environment variables.
     * When true, error messages will include environment-specific guidance.
     */
    @Builder.Default
    private final boolean configuredFromEnv = false;

    private final String cacheKey = JavaHelper.identity(this);

    public static BitbucketUnirestInstanceSupplierBuilder builder(UnirestContext unirestContext) {
        return new BitbucketUnirestInstanceSupplierBuilder().unirestContext(unirestContext);
    }

    public static BitbucketUnirestInstanceSupplier fromEnv(UnirestContext unirestContext) {
        return builder(unirestContext)
            .urlConfig(UrlConfig.builder()
                .url(EnvHelper.envOrDefault(BitbucketEnvironment.ENV_API_URL, "https://api.bitbucket.org/2.0"))
                .build())
            .oauthToken(EnvHelper.env(BitbucketEnvironment.ENV_STEP_OAUTH_TOKEN))
            .token(EnvHelper.env(BitbucketEnvironment.ENV_TOKEN))
            .username(EnvHelper.env(BitbucketEnvironment.ENV_USERNAME))
            .appPassword(EnvHelper.env(BitbucketEnvironment.ENV_APP_PASSWORD))
            .configuredFromEnv(true)
            .build();
    }

    @Override
    public UnirestInstance getUnirestInstance() {
        return unirestContext.getUnirestInstance(cacheKey, this::configureUnirest);
    }

    private void configureUnirest(UnirestInstance unirest) {
        var bearer = StringUtils.firstNonBlank(oauthToken, token);
        var hasExplicitAuth = StringUtils.isNotBlank(bearer) || 
            (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(appPassword));
        var usingProxy = !hasExplicitAuth && isBitbucketPipelines();
        
        BitbucketUnexpectedHttpResponseConfigurer.configure(unirest, 
            StringUtils.firstNonBlank(bearer, username), usingProxy, configuredFromEnv);
        UnirestJsonHeaderConfigurer.configure(unirest);
        UnirestUrlConfigConfigurer.configure(unirest, urlConfig);
        
        if (StringUtils.isNotBlank(bearer)) {
            unirest.config().setDefaultHeader("Authorization", "Bearer " + bearer);
        } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(appPassword)) {
            var basic = Base64.getEncoder().encodeToString((username + ":" + appPassword).getBytes(StandardCharsets.UTF_8));
            unirest.config().setDefaultHeader("Authorization", "Basic " + basic);
        } else if (usingProxy) {
            // In Bitbucket Pipelines without explicit credentials, use the authentication proxy
            // which automatically adds the required Auth header. The proxy runs on different
            // addresses depending on whether we're in a Docker container (pipe) or not.
            String proxyHost = FcliDockerHelper.isRunningInContainer() 
                ? "host.docker.internal" 
                : "localhost";
            unirest.config().proxy(proxyHost, 29418);
            return; // Skip standard proxy configuration when using Bitbucket auth proxy
        }
        
        ProxyHelper.configureProxy(unirest, BitbucketEnvironment.TYPE, urlConfig.getUrl());
    }
    
    private boolean isBitbucketPipelines() {
        // Detect if running in Bitbucket Pipelines by checking for pipeline-specific env var
        return StringUtils.isNotBlank(EnvHelper.env(BitbucketEnvironment.ENV_PIPELINE_UUID));
    }
}
