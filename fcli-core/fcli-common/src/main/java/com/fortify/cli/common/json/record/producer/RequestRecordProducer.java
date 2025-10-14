package com.fortify.cli.common.json.record.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IfFailureHandler;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
public class RequestRecordProducer extends AbstractTransformingRecordProducer {
    @Getter private final HttpRequest<?> initialRequest;
    @Getter private final INextPageRequestProducer nextPageRequestProducer;
    @Getter private final INextPageUrlProducer nextPageUrlProducer;
    
    @Override
    protected void produceRawInputs(RawInputConsumer consumer) {
        if ( nextPageRequestProducer!=null ) {
            PagingHelper.processPages(initialRequest, nextPageRequestProducer, r->handleResponse(r, consumer));
        } else if ( nextPageUrlProducer!=null ) {
            PagingHelper.pagedRequest(initialRequest, nextPageUrlProducer)
                .ifSuccess(r->handleResponse(r, consumer))
                .ifFailure(IfFailureHandler::handle);
        } else {
            initialRequest.asObject(JsonNode.class)
                .ifSuccess(r->handleResponse(r, consumer))
                .ifFailure(IfFailureHandler::handle);
        }
    }
    
    private void handleResponse(HttpResponse<JsonNode> r, RawInputConsumer consumer) { consumer.accept(r.getBody()); }
}
