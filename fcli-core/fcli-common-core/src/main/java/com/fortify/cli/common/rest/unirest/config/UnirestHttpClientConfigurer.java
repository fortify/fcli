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
package com.fortify.cli.common.rest.unirest.config;

import java.util.function.Consumer;

import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kong.unirest.Config;
import kong.unirest.UnirestInstance;
import kong.unirest.apache.ApacheClient;

/**
 * Centralized Apache HttpClient configuration for Unirest instances.
 * Applies the JVM default SSLContext configured during static initialization.
 */
public final class UnirestHttpClientConfigurer {
    private static final Logger log = LoggerFactory.getLogger(UnirestHttpClientConfigurer.class);

    private UnirestHttpClientConfigurer() {
    }

    public static void configure(UnirestInstance unirest, Consumer<HttpClientBuilder> customConfigurer) {
        unirest.config().httpClient(config -> createApacheClient(config, customConfigurer));
    }

    public static ApacheClient createApacheClient(Config config, Consumer<HttpClientBuilder> customConfigurer) {
        return new ApacheClient(config, cb -> {
            if (customConfigurer != null) {
                customConfigurer.accept(cb);
            }
        });
    }
}