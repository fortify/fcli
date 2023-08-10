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

import kong.unirest.core.UnirestInstance;

public class UnirestJsonHeaderConfigurer {
    public static final void configure(UnirestInstance unirest) {
        unirest.config()
            .setDefaultHeader("Accept", "application/json")
            .setDefaultHeader("Content-Type", "application/json");
    }
}
