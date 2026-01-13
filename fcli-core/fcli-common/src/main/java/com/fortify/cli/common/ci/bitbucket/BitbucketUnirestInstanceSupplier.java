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
import com.fortify.cli.common.rest.unirest.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.rest.unirest.config.UnirestUrlConfigConfigurer;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.util.EnvHelper;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.UnirestInstance;
import lombok.Builder;

/**
 * Supplies UnirestInstance instances configured for Bitbucket REST API calls,
 * handling base URL overrides, proxy settings, and authentication via OAuth
 * access token or username/app-password pairs.
 */
@Reflectable
@Builder
public class BitbucketUnirestInstanceSupplier implements IUnirestInstanceSupplier {
    private static final String TYPE = "bitbucket";
    private final UnirestContext unirestContext;

    @Builder.Default
    private final IUrlConfig urlConfig = UrlConfig.builder()
        .url(EnvHelper.envOrDefault(BitbucketEnvironment.ENV_API_URL, "https://api.bitbucket.org/2.0"))
        .build();

    @Builder.Default
    private final String oauthToken = EnvHelper.env(BitbucketEnvironment.ENV_STEP_OAUTH_TOKEN);

    @Builder.Default
    private final String token = EnvHelper.env(BitbucketEnvironment.ENV_TOKEN);

    @Builder.Default
    private final String username = EnvHelper.env(BitbucketEnvironment.ENV_USERNAME);

    @Builder.Default
    private final String appPassword = EnvHelper.env(BitbucketEnvironment.ENV_APP_PASSWORD);

    private final String cacheKey = JavaHelper.identity(this);

    public static BitbucketUnirestInstanceSupplierBuilder builder(UnirestContext unirestContext) {
        return new BitbucketUnirestInstanceSupplierBuilder().unirestContext(unirestContext);
    }

    public static BitbucketUnirestInstanceSupplier fromEnv(UnirestContext unirestContext) {
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
        ProxyHelper.configureProxy(unirest, TYPE, urlConfig.getUrl());
        var bearer = StringUtils.firstNonBlank(oauthToken, token);
        if (StringUtils.isNotBlank(bearer)) {
            unirest.config().setDefaultHeader("Authorization", "Bearer " + bearer);
        } else if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(appPassword)) {
            var basic = Base64.getEncoder().encodeToString((username + ":" + appPassword).getBytes(StandardCharsets.UTF_8));
            unirest.config().setDefaultHeader("Authorization", "Basic " + basic);
        }
    }
}
