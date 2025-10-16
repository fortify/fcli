/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.common.json.producer;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IHttpRequestUpdater;
import com.fortify.cli.common.rest.unirest.IfFailureHandler;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;
import lombok.Singular;
import lombok.experimental.SuperBuilder;

/**
 * Producer built around executing an HTTP {@link HttpRequest}. Supports request updaters and paging.
 */
@SuperBuilder
public class RequestObjectNodeProducer extends AbstractObjectNodeProducer {
    @Getter private final HttpRequest<?> initialRequest;
    @Singular private final List<IHttpRequestUpdater> requestUpdaters;
    private final INextPageRequestProducer nextPageRequestProducer;
    private final INextPageUrlProducer nextPageUrlProducer;

    @Override
    public void forEach(IObjectNodeConsumer consumer) {
        HttpRequest<?> request = applyRequestUpdaters(initialRequest);
        if ( nextPageRequestProducer!=null ) {
            PagingHelper.processPages(request, nextPageRequestProducer, r->handleResponse(r, consumer));
        } else if ( nextPageUrlProducer!=null ) {
            PagingHelper.pagedRequest(request, nextPageUrlProducer).ifSuccess(r->handleResponse(r, consumer)).ifFailure(IfFailureHandler::handle);
        } else {
            request.asObject(JsonNode.class).ifSuccess(r->handleResponse(r, consumer)).ifFailure(IfFailureHandler::handle);
        }
    }

    private HttpRequest<?> applyRequestUpdaters(HttpRequest<?> base) {
        if ( requestUpdaters==null ) { return base; }
        HttpRequest<?> current = base;
        for ( var updater : requestUpdaters ) { current = updater.updateRequest(current); }
        return current;
    }

    private void handleResponse(HttpResponse<JsonNode> r, IObjectNodeConsumer consumer) {
        process(r.getBody(), consumer);
    }

    public static class RequestObjectNodeProducerBuilderImpl extends RequestObjectNodeProducerBuilder<RequestObjectNodeProducer, RequestObjectNodeProducerBuilderImpl> {
        public RequestObjectNodeProducerBuilderImpl applyAllFromSpec() {
            super.applyAllFromSpec();
            applyRequestUpdatersFromSpec();
            applyNextPageUrlProducerFromSpec();
            return self();
        }

        private void applyNextPageUrlProducerFromSpec() {
            getAllUserObjectsStream().forEach(this::addNextPageUrlProducerFromObject);
        }

        public void applyRequestUpdatersFromSpec() {
            getAllUserObjectsStream().forEach(this::addRequestUpdaterFromObject);
        }

        private void addRequestUpdaterFromObject(Object o) {
            if (o instanceof IHttpRequestUpdater u) {
                requestUpdater(u);
            }
        }

        private void addNextPageUrlProducerFromObject(Object o) {
            if (o instanceof INextPageUrlProducerSupplier s) {
                nextPageUrlProducer(s.getNextPageUrlProducer());
            }
        }
    }
}
