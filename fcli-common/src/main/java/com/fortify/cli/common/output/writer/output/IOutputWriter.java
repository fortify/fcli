package com.fortify.cli.common.output.writer.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;

public interface IOutputWriter {

    void write(JsonNode jsonNode);

    void write(HttpRequest<?> httpRequest);

    void write(HttpRequest<?> httpRequest, INextPageUrlProducer nextPageUrlProducer);

    void write(HttpResponse<JsonNode> httpResponse);

}