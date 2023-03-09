/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 *
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.fod.lookup.helper;

import java.util.Iterator;
import java.util.List;

import javax.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod.lookup.cli.mixin.FoDLookupTypeOptions.FoDLookupType;
import com.fortify.cli.fod.rest.FoDUrls;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDLookupHelper {
    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();


    public static final JsonNode renameFields(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupType type, String text, boolean failIfNotFound) throws JsonProcessingException {
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            FoDLookupDescriptor currentLookup = lookupIterator.next();
            if (currentLookup.getText().equals(text)) return currentLookup;
        }
        if (failIfNotFound)
            throw new ValidationException("No value found for '" + text + "' in " + type.name());
        return null;
    }

    public static final FoDLookupDescriptor getDescriptor(UnirestInstance unirestInstance, FoDLookupType type,
                                                          String group, String text, boolean failIfNotFound) throws JsonProcessingException {
        GetRequest request = unirestInstance.get(FoDUrls.LOOKUP_ITEMS).queryString("type",
                type.name());
        JsonNode items = request.asObject(ObjectNode.class).getBody().get("items");
        List<FoDLookupDescriptor> lookupList = objectMapper.readValue(objectMapper.writeValueAsString(items),
                new TypeReference<List<FoDLookupDescriptor>>() {
                });
        Iterator<FoDLookupDescriptor> lookupIterator = lookupList.iterator();
        while (lookupIterator.hasNext()) {
            FoDLookupDescriptor currentLookup = lookupIterator.next();
            if (currentLookup.getGroup().equals(group) && currentLookup.getText().equals(text)) return currentLookup;
        }
        if (failIfNotFound)
            throw new ValidationException("No value found for '" + text + "' with group '" + group + "' in " + type.name());
        return null;
    }
}
