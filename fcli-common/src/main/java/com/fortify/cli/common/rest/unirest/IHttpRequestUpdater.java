package com.fortify.cli.common.rest.unirest;

import kong.unirest.HttpRequest;

public interface IHttpRequestUpdater {
    HttpRequest<?> updateRequest(HttpRequest<?> request);
}
