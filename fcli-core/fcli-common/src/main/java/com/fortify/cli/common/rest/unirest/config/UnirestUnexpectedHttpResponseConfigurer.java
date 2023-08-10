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

import com.fortify.cli.common.rest.unirest.UnexpectedHttpResponseException;

import kong.unirest.core.Config;
import kong.unirest.core.HttpRequestSummary;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Interceptor;
import kong.unirest.core.UnirestException;
import kong.unirest.core.UnirestInstance;

public class UnirestUnexpectedHttpResponseConfigurer implements Interceptor {
    
    public static final void configure(UnirestInstance unirestInstance) {
        unirestInstance.config().interceptor(UnexpectedHttpResponseInterceptor.INSTANCE);
    }
    
    private static final class UnexpectedHttpResponseInterceptor implements Interceptor {
        private static final UnexpectedHttpResponseInterceptor INSTANCE = new UnexpectedHttpResponseInterceptor();
        
        @Override
        public void onResponse(HttpResponse<?> response, HttpRequestSummary requestSummary, Config config) {
            if ( !response.isSuccess() ) {
                throw new UnexpectedHttpResponseException(response, requestSummary);
            }
        }
        
        @Override
        public HttpResponse<?> onFail(Exception e, HttpRequestSummary request, Config config) throws UnirestException {
            throw (e instanceof UnirestException) ? (UnirestException)e : new UnirestException(e); 
        }
    }
}
