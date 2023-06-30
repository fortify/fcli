/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors ("Open Text") are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.rest.unirest.config;

import java.util.function.Consumer;

import com.fortify.cli.common.util.StringUtils;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @ReflectiveAccess @NoArgsConstructor @AllArgsConstructor @Builder
public class UrlConfig implements IUrlConfig {
    private String  url;
    private Boolean insecureModeEnabled;
    
    public static final UrlConfig from(IUrlConfig other) {
        return builderFrom(other).build();
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other) {
        UrlConfigBuilder builder = UrlConfig.builder();
        if ( other!=null ) {
            builder = builder
                .url(other.getUrl())
                .insecureModeEnabled(other.isInsecureModeEnabled());
        }
        return builder;
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other, IUrlConfig overrides) {
        UrlConfigBuilder builder = other==null ? builderFrom(overrides) : builderFrom(other);
        if ( other!=null && overrides!=null ) {
            override(overrides.getUrl(), builder::url);
            override(overrides.getInsecureModeEnabled(), builder::insecureModeEnabled);
        }
        return builder;
    }
    
    private static final void override(String value, Consumer<String> setter) {
        if ( StringUtils.isNotBlank(value) ) { setter.accept(value); }
    }
    
    private static final <T extends Object> void override(T value, Consumer<T> setter) {
        if ( value!=null ) { setter.accept(value); }
    }
}
