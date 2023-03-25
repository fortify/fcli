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
package com.fortify.cli.common.rest.unirest.config;

import kong.unirest.UnirestInstance;

public final class UnirestBasicAuthConfigurer {
    /**
     * Configure the given {@link UnirestInstance} to use basic authentication based on the 
     * given {@link IUserCredentialsConfig} instance
     * @param unirestInstance {@link UnirestInstance} to be configured
     * @param userCredentialsConfig used to provide the basic authentication credentials {@link UnirestInstance}
     */
    public static final void configure(UnirestInstance unirestInstance, IUserCredentialsConfig userCredentialsConfig) {
        if ( unirestInstance==null ) { throw new IllegalArgumentException("Unirest instance may not be null"); }
        if ( userCredentialsConfig==null ) { throw new IllegalArgumentException("User credentials may not be null"); }
        unirestInstance.config()
            .setDefaultBasicAuth(userCredentialsConfig.getUser(), new String(userCredentialsConfig.getPassword()));
    }
}
