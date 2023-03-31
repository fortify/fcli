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
