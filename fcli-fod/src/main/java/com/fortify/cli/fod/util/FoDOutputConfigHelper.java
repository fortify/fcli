package com.fortify.cli.fod.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import io.micronaut.http.uri.UriBuilder;
import kong.unirest.HttpResponse;

import java.net.URI;
import java.util.function.Function;

public class FoDOutputConfigHelper {
    /**
     * Provide default table output configuration for results optionally embedded in an items object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig table() {
        return OutputConfig.table().inputTransformer(FoDOutputConfigHelper::getItemsOrSelf);
    }

    /**
     * Provide default details output configuration for results optionally embedded in a data object
     * @return {@link OutputConfig}
     */
    public static final OutputConfig details() {
        // TODO For now we use yaml output, until #104 has been fixed
        return OutputConfig.yaml().inputTransformer(FoDOutputConfigHelper::getItemsOrSelf);
    }

    public static final Function<HttpResponse<JsonNode>, String> pagingHandler(final String uri) {
        return r -> {
            JsonNode body = r.getBody();
            int offset = body.get("offset").asInt();
            int totalCount = body.get("totalCount").asInt();
            int limit = body.get("limit").asInt();
            int newOffset = offset + limit;
            if (newOffset < totalCount) {
                return UriBuilder.of(URI.create(uri)).replaceQueryParam("offset", newOffset).build().toString();
            }
            return null;
        };
    }

    private static final JsonNode getItemsOrSelf(JsonNode json) {
        return json.has("items") ? json.get("items") : json;
    }

}
