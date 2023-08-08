/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.rest.lookup.helper;

import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import com.fortify.cli.fod.rest.lookup.cli.mixin.FoDLookupTypeOptions;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDLookupHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupTypeOptions.FoDLookupType type,
                                                          String text, boolean failIfNotFound) throws JsonProcessingException {
        FoDLookupDescriptor currentLookup = null;
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            currentLookup = lookupIterator.next();
            if (currentLookup.getText().equals(text)) {
                break;
            }
        }
        if (failIfNotFound) {
            throw new IllegalArgumentException("No value found for '" + text + "' in " + type.name());
        }
        return currentLookup;
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupTypeOptions.FoDLookupType type,
                                                          String group, String text, boolean failIfNotFound) throws JsonProcessingException {
        FoDLookupDescriptor currentLookup = null;
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            currentLookup = lookupIterator.next();
            if (currentLookup.getGroup().equals(group) && currentLookup.getText().equals(text)) {
                break;
            }
        }
        if (failIfNotFound) {
            throw new IllegalArgumentException("No value found for '" + text + "' with group '" + group + "' in " + type.name());
        }
        return currentLookup;
    }
}
