package com.fortify.cli.common.output.spi.request;

import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;

public interface INextPageUrlProducerSupplier {
    INextPageUrlProducer getNextPageUrlProducer(UnirestInstance unirest, HttpRequest<?> originalRequest);
}
