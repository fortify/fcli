package com.fortify.cli.common.output.spi.request;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IHttpRequestUpdater {
    HttpRequest<?> updateRequest(UnirestInstance unirest, HttpRequest<?> request);
}
