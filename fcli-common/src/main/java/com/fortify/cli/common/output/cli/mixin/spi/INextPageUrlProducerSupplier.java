package com.fortify.cli.common.output.cli.mixin.spi;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;

public interface INextPageUrlProducerSupplier {
    default Function<HttpResponse<JsonNode>, String> getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest) {
        return null;
    }
}
