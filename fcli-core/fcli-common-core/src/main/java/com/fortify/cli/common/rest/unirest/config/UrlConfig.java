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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import com.formkiq.graalvm.annotations.Reflectable;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder
@Reflectable @NoArgsConstructor @AllArgsConstructor 
public class UrlConfig implements IUrlConfig {
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    private String  url;
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "HEADER VALUE", pattern = "[^:]+:\\s*(.+)")
    @Builder.Default
    private List<String> headers = new ArrayList<>();
    private int     socketTimeoutInMillis;
    private int     connectTimeoutInMillis;
    private Boolean insecureModeEnabled;
    
    public static final UrlConfig from(IUrlConfig other) {
        return builderFrom(other).build();
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other) {
        var builder = builderFromConnectionConfig(other);
        if (null != other) {
            builder = builder.url(other.getUrl());
        }
        return builder;
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other, IUrlConfig overrides) {
        UrlConfigBuilder builder = other==null ? builderFrom(overrides) : builderFrom(other);
        if ( other!=null && overrides!=null ) {
            override(overrides.getUrl(), builder::url);
            override(overrides.getHeaders(), builder::headers);
            override(overrides.getInsecureModeEnabled(), builder::insecureModeEnabled);
            builder.connectTimeoutInMillis(overrides.getConnectTimeoutInMillis())
                .socketTimeoutInMillis(overrides.getSocketTimeoutInMillis());
        }
        return builder;
    }
    
    public static final UrlConfigBuilder builderFromConnectionConfig(IConnectionConfig other) {
        UrlConfigBuilder builder = UrlConfig.builder();
        if ( other!=null ) {
            builder = builder
                .headers(other.getHeaders()==null ? new ArrayList<>() : new ArrayList<>(other.getHeaders()))
                .insecureModeEnabled(other.isInsecureModeEnabled())
                .connectTimeoutInMillis(other.getConnectTimeoutInMillis())
                .socketTimeoutInMillis(other.getSocketTimeoutInMillis());
        }
        return builder;
    }
    
    private static final void override(String value, Consumer<String> setter) {
        if ( StringUtils.isNotBlank(value) ) { setter.accept(value); }
    }
    
    private static final <T extends Object> void override(T value, Consumer<T> setter) {
        if ( value!=null ) { setter.accept(value); }
    }

    private static final void override(List<String> value, Consumer<List<String>> setter) {
        if ( value!=null && !value.isEmpty() ) {
            setter.accept(new ArrayList<>(value));
        }
    }
}
