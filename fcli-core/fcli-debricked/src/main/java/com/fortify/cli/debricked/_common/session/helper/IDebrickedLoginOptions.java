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
package com.fortify.cli.debricked._common.session.helper;

import com.fortify.cli.common.rest.unirest.config.IUserCredentialsConfig;

public interface IDebrickedLoginOptions {
    IDebrickedUrlConfigOptions getUrlConfigOptions();
    IDebrickedAuthOptions getAuthOptions();
    
    interface IDebrickedAuthOptions {
        IDebrickedUserCredentialOptions getUserCredentialsOptions();
        IDebrickedAccessTokenCredentialOptions getTokenOptions();
    }
    
    interface IDebrickedUserCredentialOptions extends IUserCredentialsConfig {
        String getUser();
        char[] getPassword();
    }
    
    interface IDebrickedAccessTokenCredentialOptions {
        char[] getAccessToken();
    }
}