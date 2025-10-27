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
    @Getter private final HttpRequest<?> baseRequest;
    /** Optional unirest instance; if provided and only a {@link INextPageUrlProducer} is configured, we convert it
     *  to an {@link INextPageRequestProducer} to stream pages instead of collecting them. */
    @Getter private final kong.unirest.UnirestInstance unirestInstance;
    @Singular private final List<IHttpRequestUpdater> requestUpdaters;
    private final INextPageRequestProducer nextPageRequestProducer;
    private final INextPageUrlProducer nextPageUrlProducer;
    // Test-only support: if configured, simulate multi-page responses without performing HTTP requests
    @Singular private final java.util.List<JsonNode> testPageBodies;

    @Override
    public void forEach(IObjectNodeConsumer consumer) {
        // Test-mode shortcut: simulate paging if testPageBodies configured
        if ( testPageBodies!=null && !testPageBodies.isEmpty() ) {
            for ( var body : testPageBodies ) { process(body, consumer); }
            return;
        }
        HttpRequest<?> request = applyRequestUpdaters(baseRequest);
        INextPageRequestProducer effectiveNextPageRequestProducer = nextPageRequestProducer;
        if ( effectiveNextPageRequestProducer==null && nextPageUrlProducer!=null && unirestInstance!=null ) {
            effectiveNextPageRequestProducer = PagingHelper.asNextPageRequestProducer(unirestInstance, nextPageUrlProducer);
        }
        if ( effectiveNextPageRequestProducer!=null ) {
            PagingHelper.processPages(request, effectiveNextPageRequestProducer, r->handleResponse(r, consumer));
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
        public RequestObjectNodeProducerBuilderImpl applyAllFrom(ObjectNodeProducerApplyFrom applyFrom) {
            super.applyAllFrom(applyFrom);
            // Auto-apply Unirest instance if command supplies it
            var ch = getRequiredCommandHelper();
            ch.getCommandAs(com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier.class)
                .ifPresent(s -> super.unirestInstance(s.getUnirestInstance()));
            applyRequestUpdatersFrom(applyFrom);
            applyNextPageUrlProducerFrom(applyFrom);
            return self();
        }

        private void applyNextPageUrlProducerFrom(ObjectNodeProducerApplyFrom applyFrom) {
            applyFrom.getSourceStream(getRequiredCommandHelper(), getExplicitProductHelper()).forEach(this::addNextPageUrlProducerFromObject);
        }

        public void applyRequestUpdatersFrom(ObjectNodeProducerApplyFrom applyFrom) {
            applyFrom.getSourceStream(getRequiredCommandHelper(), getExplicitProductHelper()).forEach(this::addRequestUpdaterFromObject);
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

        /** Configure unirest instance to enable streaming paging conversion. */
        public RequestObjectNodeProducerBuilderImpl unirestInstance(kong.unirest.UnirestInstance unirestInstance) {
            return (RequestObjectNodeProducerBuilderImpl)super.unirestInstance(unirestInstance);
        }
    }
}
