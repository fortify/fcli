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
package com.fortify.cli.aviator.ssc.helper;

import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fortify.cli.aviator.ssc.helper.AviatorSSCTagDefs.TagDefinition;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;

/**
 * Reads SSC custom tag JSON for prepare and pre-upload validation. HTTP failures are not
 * handled here, so each caller keeps its own error semantics.
 */
final class AviatorSSCTagJsonHelper {
    private static final String SYSTEM_MANAGED_TAG_TYPE = "AVIATOR";

    private AviatorSSCTagJsonHelper() {}

    /**
     * Returns the {@code /internalCustomTags} tags, or {@code null} when the response has no data array.
     * HTTP failures, including 404/405 from SSC versions without this endpoint, propagate to the caller.
     */
    static ArrayNode fetchInternalCustomTags(UnirestInstance unirest) {
        return dataArray(unirest.get(SSCUrls.INTERNAL_CUSTOM_TAGS).asObject(JsonNode.class).getBody());
    }

    /**
     * Returns whether {@code tag} is system-managed, meaning its custom tag type is {@code AVIATOR}.
     */
    static boolean isSystemManaged(JsonNode tag) {
        return tag != null && SYSTEM_MANAGED_TAG_TYPE.equalsIgnoreCase(tag.path("customTagType").asText());
    }

    /**
     * Returns whether {@code tag} is {@code tagDef} and is system-managed.
     */
    static boolean isSystemManaged(JsonNode tag, TagDefinition tagDef) {
        return isSystemManaged(tag) && tagDef.getGuid().equalsIgnoreCase(tag.path("guid").asText());
    }

    /**
     * Returns the first tag whose GUID matches {@code guid}, ignoring case, or {@code null} when none does.
     */
    static JsonNode findByGuid(ArrayNode tags, String guid) {
        if (tags == null || guid == null) {
            return null;
        }
        return JsonHelper.stream(tags)
            .filter(tag -> guid.equalsIgnoreCase(tag.path("guid").asText()))
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the {@code lookupValue} entries of {@code tag}'s value list, or {@code null} when the
     * value list is absent, so callers can tell an unknown list from an empty one.
     */
    static Set<String> lookupValues(JsonNode tag) {
        JsonNode valueList = tag == null ? null : tag.get("valueList");
        if (valueList == null || !valueList.isArray()) {
            return null;
        }
        return JsonHelper.stream((ArrayNode) valueList)
            .map(value -> value.path("lookupValue").asText())
            .collect(Collectors.toSet());
    }

    /**
     * Returns the {@code data} array from an SSC response, or {@code null} when that array is absent.
     */
    static ArrayNode dataArray(JsonNode body) {
        JsonNode data = body == null ? null : body.get("data");
        return data != null && data.isArray() ? (ArrayNode) data : null;
    }
}
