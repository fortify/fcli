package com.fortify.cli.common.rest.paging;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.HttpResponse;

public interface INextPageUrlProducer {
    String getNextPageUrl(HttpResponse<JsonNode> jsonNodeResponse);
}
