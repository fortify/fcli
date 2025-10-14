package com.fortify.cli.common.json.record.producer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageRequestProducer;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;
import com.fortify.cli.common.rest.paging.PagingHelper;
import com.fortify.cli.common.rest.unirest.IfFailureHandler;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;

public class RequestRecordProducer extends AbstractTransformingRecordProducer {
    private final HttpRequest<?> initialRequest;
    private final INextPageRequestProducer nextPageRequestProducer;
    private final INextPageUrlProducer nextPageUrlProducer;
    
    public RequestRecordProducer(StandardOutputConfig outputConfig, HttpRequest<?> initialRequest, INextPageRequestProducer nextPageRequestProducer, INextPageUrlProducer nextPageUrlProducer) {
        super(outputConfig);
        this.initialRequest = initialRequest;
        this.nextPageRequestProducer = nextPageRequestProducer;
        this.nextPageUrlProducer = nextPageUrlProducer;
    }
    
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
