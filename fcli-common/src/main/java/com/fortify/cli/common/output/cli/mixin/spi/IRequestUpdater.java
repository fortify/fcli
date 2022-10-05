package com.fortify.cli.common.output.cli.mixin.spi;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface IRequestUpdater {
    default HttpRequest<?> updateRequest(UnirestInstance unirest, HttpRequest<?> request) { return request; }
}
