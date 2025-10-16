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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

/**
 * Producer built around executing an HTTP {@link HttpRequest}. Supports request updaters and paging.
 */
public class RequestObjectNodeProducer extends AbstractObjectNodeProducer {
    @Getter private final HttpRequest<?> initialRequest;
    private final Iterable<IHttpRequestUpdater> requestUpdaters;
    private final INextPageRequestProducer nextPageRequestProducer;
    private final INextPageUrlProducer nextPageUrlProducer;

    protected RequestObjectNodeProducer(RequestObjectNodeProducerBuilder b) {
        super(b.inputTransformers, b.recordTransformers, b.queryFilterStage);
        this.initialRequest = b.initialRequest;
        this.requestUpdaters = b.requestUpdaters;
        this.nextPageRequestProducer = b.nextPageRequestProducer;
        this.nextPageUrlProducer = b.nextPageUrlProducer;
    }

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

    public static class RequestObjectNodeProducerBuilder extends AbstractObjectNodeProducerBuilder<RequestObjectNodeProducer, RequestObjectNodeProducerBuilder> {
        private HttpRequest<?> initialRequest;
        private List<IHttpRequestUpdater> requestUpdaters = new ArrayList<>();
        private INextPageRequestProducer nextPageRequestProducer;
        private INextPageUrlProducer nextPageUrlProducer;
        public static RequestObjectNodeProducerBuilder builder() { return new RequestObjectNodeProducerBuilder(); }
        public RequestObjectNodeProducerBuilder initialRequest(HttpRequest<?> r) { this.initialRequest = r; return self(); }
        public RequestObjectNodeProducerBuilder requestUpdater(IHttpRequestUpdater upd) { if (upd!=null) { requestUpdaters.add(upd); } return self(); }
        public RequestObjectNodeProducerBuilder nextPageRequestProducer(INextPageRequestProducer p) { this.nextPageRequestProducer = p; return self(); }
        public RequestObjectNodeProducerBuilder nextPageUrlProducer(INextPageUrlProducer p) { this.nextPageUrlProducer = p; return self(); }
        public RequestObjectNodeProducerBuilder applyFromSpec() {
            super.applyFromSpec();
            // Apply request updaters from all user objects (product helper, command spec, mixins)
            getAllUserObjectsStream().forEach(this::addRequestUpdaterFromObject);
            // Determine paging producer if not already set; first match wins
            if ( nextPageUrlProducer==null ) {
                nextPageUrlProducer = getAllUserObjectsStream()
                        .map(this::nextPageUrlProducerFromObject)
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
            return self();
        }
        private void addRequestUpdaterFromObject(Object o) { if ( o instanceof IHttpRequestUpdater u ) { requestUpdater(u); } }
        private INextPageUrlProducer nextPageUrlProducerFromObject(Object o) { return o instanceof INextPageUrlProducerSupplier s ? s.getNextPageUrlProducer() : null; }
        @Override protected RequestObjectNodeProducerBuilder self() { return this; }
        @Override public RequestObjectNodeProducer build() { return new RequestObjectNodeProducer(this); }
    }
}
