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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.json.producer.JsonNodeProducers.ObjectNodeProducer;
import com.fortify.cli.common.output.processing.IRecordProducerConfig;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IfFailureHandler;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;

public class RequestRecordProducer implements ObjectNodeProducer {
    @Getter private final HttpRequest<?> initialRequest;
    @Getter private final INextPageRequestProducer nextPageRequestProducer;
    @Getter private final INextPageUrlProducer nextPageUrlProducer;
    private final TransformationPipelineRunner runner;
    public RequestRecordProducer(IRecordProducerConfig recordProducerConfig, HttpRequest<?> initialRequest,
            INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
        this.initialRequest = initialRequest;
        this.nextPageRequestProducer = nextPageRequestProducer;
        this.nextPageUrlProducer = nextPageUrlProducer;
        this.runner = new TransformationPipelineRunner(recordProducerConfig);
    }
    @Override
    public void forEach(JsonNodeConsumers.ObjectNodeConsumer consumer) {
        if (nextPageRequestProducer != null) {
            PagingHelper.processPages(initialRequest, nextPageRequestProducer, r -> handleResponse(r, consumer));
        } else if (nextPageUrlProducer != null) {
            PagingHelper.pagedRequest(initialRequest, nextPageUrlProducer).ifSuccess(r -> handleResponse(r, consumer))
                    .ifFailure(IfFailureHandler::handle);
        } else {
            initialRequest.asObject(JsonNode.class).ifSuccess(r -> handleResponse(r, consumer)).ifFailure(IfFailureHandler::handle);
        }
    }
    private void handleResponse(HttpResponse<JsonNode> r, JsonNodeConsumers.ObjectNodeConsumer consumer) { runner.process(r.getBody(), consumer::accept); }
}
