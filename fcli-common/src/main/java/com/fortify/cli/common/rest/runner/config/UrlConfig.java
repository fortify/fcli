/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.common.rest.runner.config;

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
    private int     socketTimeoutInMillis;
    private int     connectTimeoutInMillis;
    private Boolean insecureModeEnabled;
    
    public static final UrlConfig from(IUrlConfig other) {
        return builderFrom(other).build();
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other) {
        UrlConfigBuilder builder = UrlConfig.builder();
        if ( other!=null ) {
            builder = builder
                .url(other.getUrl())
                .insecureModeEnabled(other.isInsecureModeEnabled())
                .connectTimeoutInMillis(other.getConnectTimeoutInMillis())
                .socketTimeoutInMillis(other.getSocketTimeoutInMillis());
        }
        return builder;
    }
    
    public static final UrlConfigBuilder builderFrom(IUrlConfig other, IUrlConfig overrides) {
        UrlConfigBuilder builder = other==null ? builderFrom(overrides) : builderFrom(other);
        if ( other!=null && overrides!=null ) {
            override(overrides.getUrl(), builder::url);
            override(overrides.getInsecureModeEnabled(), builder::insecureModeEnabled);
            builder.connectTimeoutInMillis(overrides.getConnectTimeoutInMillis())
                .socketTimeoutInMillis(overrides.getSocketTimeoutInMillis());
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
