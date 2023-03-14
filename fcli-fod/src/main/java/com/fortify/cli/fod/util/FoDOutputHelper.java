package com.fortify.cli.fod.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.writer.output.standard.StandardOutputConfig;
import com.fortify.cli.common.rest.paging.INextPageUrlProducer;

import io.micronaut.http.uri.UriBuilder;

//TODO None of the methods in this class seem to be used anymore; if so, class should be removed
public class FoDOutputHelper {
    public static final StandardOutputConfig defaultTableOutputConfig() {
        return StandardOutputConfig.table().inputTransformer(json -> json.get("items"));
    }

    public static final INextPageUrlProducer pagingHandler(final String uri) {
        return r -> {
            JsonNode body = r.getBody();
            int offset = body.get("offset").asInt();
            int totalCount = body.get("totalCount").asInt();
            int limit = body.get("limit").asInt();
            int newOffset = offset + limit;
            if (newOffset < totalCount) {
                return UriBuilder.of(uri).replaceQueryParam("offset", newOffset).build().toString();
            }
            return null;
        };
    }
}
