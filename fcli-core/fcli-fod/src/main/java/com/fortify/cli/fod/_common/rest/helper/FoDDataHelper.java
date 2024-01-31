/**
 * Copyright 2023 Open Text.
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
package com.fortify.cli.fod._common.rest.helper;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.HttpRequest;
import kong.unirest.HttpResponse;

/**
 *
 * @author Ruud Senden
 */
public class FoDDataHelper {
    public static final ArrayNode findMatching(HttpRequest<?> request, String... filters) {
        request = request.queryString("filters", String.join("+", filters));
        Predicate<JsonNode> predicate = Stream.of(filters).map(FoDDataHelper::asPredicate).reduce(x->true, Predicate::and);
        return findMatching(request, predicate);
    }
    public static final ArrayNode findMatching(HttpRequest<?> request, Predicate<JsonNode> predicate) {
        return FoDPagingHelper.pagedRequest(request).stream()
            .map(HttpResponse::getBody)
            .map(FoDInputTransformer::getItems)
            .map(ArrayNode.class::cast)
            .flatMap(JsonHelper::stream)
            .filter(predicate)
            .collect(JsonHelper.arrayNodeCollector());
    }

    public static final JsonNode findUnique(HttpRequest<?> request, String... filters) {
        return getUnique(findMatching(request, filters));
    }

    public static final JsonNode findUnique(HttpRequest<?> request, Predicate<JsonNode> predicate) {
        return getUnique(findMatching(request, predicate));
    }

    public static final JsonNode getUnique(ArrayNode nodes) {
        switch (nodes.size()) {
        case 0: return null;
        case 1: return nodes.get(0);
        default: throw new IllegalStateException("Multiple matches found");
        }
    }

    private static final Predicate<JsonNode> asPredicate(String filter) {
        int idx = filter.indexOf(':');
        String key = filter.substring(0, idx);
        String value = filter.substring(idx+1);
        return node -> matches(node, key, value);
    }

    private static final boolean matches(JsonNode node, String fieldName, String valueToMatch) {
        JsonNode valueNode = node==null ? null : node.get(fieldName);
        return valueNode==null ? false : valueToMatch.equals(valueNode.asText());
    }
}
