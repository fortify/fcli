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
package com.fortify.cli.common.json.transform.fields;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.expression.spel.SpelEvaluationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.json.transform.AbstractJsonNodeTransformer;

public final class SelectedFieldsTransformer extends AbstractJsonNodeTransformer {
    private static final JsonNode NA_NODE = new TextNode("N/A");
    private final Map<String, String> propertyNames;

    public SelectedFieldsTransformer(Map<String, String> propertyNames, boolean supportNestedArrays) {
        super(supportNestedArrays);
        this.propertyNames = propertyNames;
    }
    public SelectedFieldsTransformer(String propertyNamesString, boolean supportNestedArrays) {
        this(parsePropertyNames(propertyNamesString), supportNestedArrays);
    }

    @Override
    public ObjectNode transformObjectNode(ObjectNode input) {
        if (propertyNames == null) {
            return input;
        }
        var result = JsonHelper.getObjectMapper().createObjectNode();
        propertyNames.entrySet().forEach(e -> result.set(e.getValue(), evaluateValue(input, e.getKey())));
        return result;
    }

    private static final JsonNode evaluateValue(ObjectNode record, String path) {
        try {
            JsonNode result = JsonHelper.evaluateSpelExpression(record, path, JsonNode.class);
            return result != null ? result : NA_NODE;
        } catch (SpelEvaluationException e) {
            // TODO Log exception
            return NA_NODE;
        }
    }

    // TODO Move this method to more logical place for re-use in MCP Server
    // implementation?
    public static final Map<String, String> parsePropertyNames(String propertyNamesString) {
        return propertyNamesString == null
                ? null
                : Arrays.stream(propertyNamesString.split("\\s*,\\s*")).map(kv -> kv.split(":")) // TODO Handle multiple colon-signs in
                                                                                                // single entry?
                        .collect(Collectors.toMap(kv -> kv[0], kv -> kv.length == 1 ? kv[0] : kv[1], (v1, v2) -> v1, LinkedHashMap::new));
    }
}
