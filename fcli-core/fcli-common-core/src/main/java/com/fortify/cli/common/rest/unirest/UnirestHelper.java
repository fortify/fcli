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
package com.fortify.cli.common.rest.unirest;

import java.io.File;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

import javax.net.ssl.SSLContext;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.rest.unirest.config.UnirestHttpClientConfigurer;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;
import lombok.SneakyThrows;

/**
 * This class provides utility methods related to Unirest
 */
public class UnirestHelper {
    public static final File download(String fcliModule, String url, File dest) {
        var parsedUrl = parseRemoteUrl(url);
        try (var unirest = createUnirestInstance()) {
            ProxyHelper.configureProxy(unirest, fcliModule, parsedUrl.getRequestUrl());
            var request = unirest.get(parsedUrl.getRequestUrl());
            parsedUrl.getHeaders().forEach(request::headerReplace);
            request.asFile(dest.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
            return dest;
        }
    }

    private static RemoteUrlAuthHelper.ParsedRemoteUrl parseRemoteUrl(String url) {
        try {
            return RemoteUrlAuthHelper.parse(url);
        } catch (Exception e) {
            throw new FcliSimpleException("Invalid URL: "+url, e);
        }
    }

    /**
     * Create a new Unirest instance, configured with the standard FCLI JSON object mapper
     * and the JVM default SSL context.
     * Callers are responsible for closing the returned instance.
     */
    @SneakyThrows
    public static UnirestInstance createUnirestInstance() {
        UnirestInstance instance = Unirest.spawnInstance();
        instance.config().setObjectMapper(new JacksonObjectMapper(JsonHelper.getObjectMapper()));
        instance.config().sslContext(SSLContext.getDefault());
        UnirestHttpClientConfigurer.configure(instance, null);
        return instance;
    }

    /**
     * Create a new Unirest instance, configured with the standard FCLI JSON object mapper
     * and any custom configuration applied by the given configurer.
     * Callers are responsible for closing the returned instance.
     */
    public static UnirestInstance createUnirestInstance(Consumer<UnirestInstance> configurer) {
        UnirestInstance instance = createUnirestInstance();
        if (configurer != null) {
            configurer.accept(instance);
        }
        return instance;
    }
}
