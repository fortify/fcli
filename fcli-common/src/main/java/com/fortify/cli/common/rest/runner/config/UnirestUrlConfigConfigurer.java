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

import kong.unirest.UnirestInstance;

public final class UnirestUrlConfigConfigurer {
    /**
     * Configure the given {@link UnirestInstance} based on the given {@link IUrlConfig} instance
     * @param unirestInstance {@link UnirestInstance} to be configured
     * @param urlConfig used to configure the {@link UnirestInstance}
     */
    public static final void configure(UnirestInstance unirestInstance, IUrlConfig urlConfig) {
        if ( unirestInstance==null ) { throw new IllegalArgumentException("Unirest instance may not be null"); }
        if ( urlConfig==null ) { throw new IllegalArgumentException("URL configuration may not be null"); }
        unirestInstance.config()
            .defaultBaseUrl(normalizeUrl(urlConfig.getUrl()))
            .verifySsl(!urlConfig.isInsecureModeEnabled())
            .socketTimeout(urlConfig.getSocketTimeoutInMillis())
            .connectTimeout(urlConfig.getConnectTimeoutInMillis());
    }
    
    /**
     * Configure the given {@link UnirestInstance} based on the {@link IUrlConfig} returned 
     * by the given {@link IUrlConfigSupplier} instance.
     * @param unirestInstance {@link UnirestInstance} to be configured
     * @param urlConfigSupplier used to configure the {@link UnirestInstance}
     */
    public static final void configure(UnirestInstance unirestInstance, IUrlConfigSupplier urlConfigSupplier) {
        if ( urlConfigSupplier==null ) { throw new IllegalArgumentException("URL configuration provider may not be null"); }
        configure(unirestInstance, urlConfigSupplier.getUrlConfig());
    }
    
    private static final String normalizeUrl(String url) {
        // We remove any trailing slashes, assuming that most users will specify relative URL's starting with /
        return url.replaceAll("/+$", "");
    }
}
