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
package com.fortify.cli.sc_dast.rest.query.cli.mixin;

import java.util.Map;

import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;

import kong.unirest.HttpRequest;
import picocli.CommandLine.Option;

public class SCDastQueryParamsMixin implements IHttpRequestUpdater {
    @Option(names="--server-queries", split=",", descriptionKey="fcli.sc-dast.server-queries", paramLabel="<name=value>")
    private Map<String,Object> serverQueries;
    
    @Override
    public HttpRequest<?> updateRequest(HttpRequest<?> request) {
        if ( serverQueries!=null && !serverQueries.isEmpty() ) {
            request = request.queryString(serverQueries);
        }
        return request;
    }
}
