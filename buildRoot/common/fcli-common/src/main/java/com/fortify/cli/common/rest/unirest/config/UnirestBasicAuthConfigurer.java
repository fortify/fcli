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
