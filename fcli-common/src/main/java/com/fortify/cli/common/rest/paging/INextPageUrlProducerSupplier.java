package com.fortify.cli.common.rest.paging;

import kong.unirest.HttpRequest;

public interface INextPageUrlProducerSupplier {
    INextPageUrlProducer getNextPageUrlProducer(HttpRequest<?> originalRequest);
}
