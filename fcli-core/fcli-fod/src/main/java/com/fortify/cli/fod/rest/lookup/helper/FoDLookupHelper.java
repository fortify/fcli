/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.fod.rest.lookup.helper;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.json.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDLookupHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupType type,
                                                        String text, boolean failIfNotFound) throws JsonProcessingException {
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        FoDLookupDescriptor foundLookup = null;
        for (FoDLookupDescriptor current : lookupList) {
            if (current.getText().equals(text) || current.getValue().equals(text)) {
                foundLookup = current;
                break;
            }
        }
        if (foundLookup == null && failIfNotFound) {
            var allowedValues = lookupList.stream().map(FoDLookupDescriptor::getText).collect(Collectors.joining(", "));
            throw new FcliSimpleException(String.format("No value found for '%s' in %s (case-sensitive). Allowed values: %s", text, type.name(), allowedValues));
        }
        return foundLookup;
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupType type,
                                                        String group, String text, boolean failIfNotFound) throws JsonProcessingException {
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        FoDLookupDescriptor foundLookup = null;
        for (FoDLookupDescriptor current : lookupList) {
            if (current.getGroup().equals(group) && current.getText().equals(text)) {
                foundLookup = current;
                break;
            }
        }
        if (foundLookup == null && failIfNotFound) {
            var allowedValues = lookupList.stream()
                    .filter(d -> d.getGroup().equals(group))
                    .map(FoDLookupDescriptor::getText)
                    .collect(Collectors.joining(", "));
            throw new FcliSimpleException(String.format("No value found for '%s' with group '%s' in %s. Allowed values: %s", text, group, type.name(), allowedValues));
        }
        return foundLookup;
    }
}
