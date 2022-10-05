package com.fortify.cli.common.output.writer.output;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;

public interface IOutputWriter {

    void write(JsonNode jsonNode);

    void write(HttpRequest<?> httpRequest);

    void write(HttpRequest<?> httpRequest, Function<HttpResponse<JsonNode>, String> nextPageUrlProducer);

    void write(HttpResponse<JsonNode> httpResponse);

}