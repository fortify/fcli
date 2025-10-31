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
package com.fortify.cli.common.action.runner.processor;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.util.JavaHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestException;
import kong.unirest.UnirestInstance;
import lombok.Data;
import lombok.RequiredArgsConstructor;

public interface IActionRequestHelper extends AutoCloseable {
    public UnirestInstance getUnirestInstance();
    public JsonNode transformInput(JsonNode input);
    public void executePagedRequest(ActionRequestDescriptor requestDescriptor);
    public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptor);
    public void close();
    
    @Data
    public static final class ActionRequestDescriptor {
        private final String method;
        private final String uri;
        private final Map<String, Object> queryParams;
        private final Object body;
        private final Consumer<JsonNode> responseConsumer;
        private final Consumer<UnirestException> failureConsumer;
        private Runnable prePageLoad;
        private Runnable postPageLoad;
        private Runnable postPageProcess;
        
        public void prePageLoad() {
            run(prePageLoad);
        }
        public void postPageLoad() {
            run(postPageLoad);
        }
        public void postPageProcess() {
            run(postPageProcess);
        }
        private void run(Runnable runnable) {
            if ( runnable!=null ) { runnable.run(); }
        }
    }
    
    @RequiredArgsConstructor
    public static class BasicActionRequestHelper implements IActionRequestHelper {
        private final IUnirestInstanceSupplier unirestInstanceSupplier;
        private final IProductHelper productHelper;
        private UnirestInstance unirestInstance;
        public final UnirestInstance getUnirestInstance() {
            if ( unirestInstance==null ) {
                unirestInstance = unirestInstanceSupplier.getUnirestInstance();
            }
            return unirestInstance;
        }
        
        @Override
        public JsonNode transformInput(JsonNode input) {
            return JavaHelper.as(productHelper, IInputTransformer.class).orElse(i->i).transformInput(input);
        }
        @Override
        public void executePagedRequest(ActionRequestDescriptor requestDescriptor) {
            var unirest = getUnirestInstance();
            INextPageUrlProducer nextPageUrlProducer = (req, resp)->{
                var nextPageUrl = JavaHelper.as(productHelper, INextPageUrlProducerSupplier.class).get()
                        .getNextPageUrlProducer().getNextPageUrl(req, resp);
                if ( nextPageUrl!=null ) {
                    requestDescriptor.prePageLoad();
                }
                return nextPageUrl;
            };
            HttpRequest<?> request = createRequest(unirest, requestDescriptor);
            requestDescriptor.prePageLoad();
            try {
                PagingHelper.processPages(unirest, request, nextPageUrlProducer, r->{
                    requestDescriptor.postPageLoad();
                    requestDescriptor.getResponseConsumer().accept(r.getBody());
                    requestDescriptor.postPageProcess();
                });
            } catch ( UnirestException e ) {
                requestDescriptor.getFailureConsumer().accept(e);
            }
        }
        @Override
        public void executeSimpleRequests(List<ActionRequestDescriptor> requestDescriptors) {
            var unirest = getUnirestInstance();
            requestDescriptors.forEach(r->executeSimpleRequest(unirest, r));
        }
        private void executeSimpleRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
            try {
                createRequest(unirest, requestDescriptor)
                    .asObject(JsonNode.class)
                    .ifSuccess(r->requestDescriptor.getResponseConsumer().accept(r.getBody()));
            } catch ( UnirestException e ) {
                requestDescriptor.getFailureConsumer().accept(e);
            }
        }

        private HttpRequest<?> createRequest(UnirestInstance unirest, ActionRequestDescriptor r) {
            var result = unirest.request(r.getMethod(), r.getUri())
                .queryString(r.getQueryParams());
            return r.getBody()==null ? result : result.body(r.getBody());
        }

        @Override
        public void close() {
            if ( unirestInstance!=null ) {
                unirestInstance.close();
            }
        }
    }
}