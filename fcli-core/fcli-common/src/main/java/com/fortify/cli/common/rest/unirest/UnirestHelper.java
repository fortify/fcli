/*
 * Copyright 2021-2025 Open Text.
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

import com.fortify.cli.common.http.proxy.helper.ProxyHelper;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import kong.unirest.jackson.JacksonObjectMapper;

/**
 * This class provides utility methods related to Unirest
 */
public class UnirestHelper {
    public static final File download(String fcliModule, String url, File dest) {
        try (var unirest = createUnirestInstance()) {
            ProxyHelper.configureProxy(unirest, fcliModule, url);
            unirest.get(url).asFile(dest.getAbsolutePath(), StandardCopyOption.REPLACE_EXISTING).getBody();
            return dest;
        }
    }

    /**
     * Create a new Unirest instance, configured with the standard FCLI JSON object mapper.
     * Callers are responsible for closing the returned instance.
     */
    public static UnirestInstance createUnirestInstance() {
        UnirestInstance instance = Unirest.spawnInstance();
        instance.config().setObjectMapper(new JacksonObjectMapper(JsonHelper.getObjectMapper()));
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
