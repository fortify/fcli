/*
 * Copyright 2021-2026 Open Text.
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

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.action.model.ActionStepRestCallEntry.ActionStepRestCallResponseType;
import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.product.IProductHelper;
import com.fortify.cli.common.output.transform.IInputTransformer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducerSupplier;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.unirest.RestResponseBodyHelper;
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
        private final ActionStepRestCallResponseType responseType;
        /** Resolved destination for {@link ActionStepRestCallResponseType#file}, null otherwise */
        private final Path responseFile;
        private final Consumer<JsonNode> responseConsumer;
        private final Consumer<RuntimeException> failureConsumer;
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
            executeSingleRequest(unirest, requestDescriptor);
        }
        
        /**
         * Execute a single (non-paged) request, handling the response body according to the
         * descriptor's response type, and pass the result or failure to the descriptor's consumers.
         */
        protected final void executeSingleRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
            if ( requestDescriptor.getResponseType()==ActionStepRestCallResponseType.auto ) {
                executeJsonRequest(unirest, requestDescriptor);
            } else {
                executeNonJsonRequest(unirest, requestDescriptor);
            }
        }
        
        private void executeJsonRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
            try {
                createRequest(unirest, requestDescriptor)
                    .asObject(JsonNode.class)
                    .ifSuccess(r->requestDescriptor.getResponseConsumer().accept(r.getBody()));
            } catch ( UnirestException e ) {
                requestDescriptor.getFailureConsumer().accept(e);
            }
        }
        
        private void executeNonJsonRequest(UnirestInstance unirest, ActionRequestDescriptor requestDescriptor) {
            JsonNode result;
            try {
                var request = createRequest(unirest, requestDescriptor);
                result = switch (requestDescriptor.getResponseType()) {
                    case file -> RestResponseBodyHelper.saveToFile(request, requestDescriptor.getResponseFile(), null).asObjectNode();
                    default -> throw new FcliBugException("Unsupported response type: "+requestDescriptor.getResponseType());
                };
            } catch ( RuntimeException e ) {
                // Besides UnirestException, this includes fcli exceptions like a missing destination directory
                requestDescriptor.getFailureConsumer().accept(e);
                return;
            }
            // Invoked outside the try block, so failures in response processing are not reported as request failures
            requestDescriptor.getResponseConsumer().accept(result);
        }

        protected final HttpRequest<?> createRequest(UnirestInstance unirest, ActionRequestDescriptor r) {
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