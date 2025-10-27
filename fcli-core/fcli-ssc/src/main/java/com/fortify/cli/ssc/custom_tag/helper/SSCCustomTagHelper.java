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
package com.fortify.cli.ssc.custom_tag.helper;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.exception.FcliTechnicalException;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;

public class SSCCustomTagHelper {
    private final UnirestInstance unirest;
    @Getter(lazy = true) private final List<SSCCustomTagDescriptor> descriptors = loadDescriptors();

    public SSCCustomTagHelper(UnirestInstance unirest) {
        this.unirest = unirest;
    }
    
    public static final List<SSCCustomTagDescriptor> toDescriptors(JsonNode tagsNode) {
        if ( tagsNode!=null && tagsNode instanceof ObjectNode ) {
            tagsNode = tagsNode.get("data");
        }
        if ( tagsNode==null || !(tagsNode instanceof ArrayNode)) { 
            throw new FcliTechnicalException("Invalid custom tags data: "+tagsNode); 
        }
        return JsonHelper.stream((ArrayNode)tagsNode)
                .map(tag -> JsonHelper.treeToValue(tag, SSCCustomTagDescriptor.class))
                .toList();
    }

    private List<SSCCustomTagDescriptor> loadDescriptors() {
        return toDescriptors(
                unirest.get(SSCUrls.CUSTOM_TAGS).queryString("limit", "-1").asObject(JsonNode.class).getBody()
        );
    }

    public SSCCustomTagDescriptor getDescriptorByCustomTagSpec(String customTagSpec, boolean failIfNotFound) {
        if (customTagSpec == null || customTagSpec.isEmpty()) {
            if (failIfNotFound) {
                throw new FcliSimpleException("Custom tag not found: null or empty");
            }
            return null;
        }
        return getDescriptors().stream()
                .filter(desc -> customTagSpec.equalsIgnoreCase(desc.getGuid())
                        || customTagSpec.equalsIgnoreCase(desc.getName())
                        || customTagSpec.equalsIgnoreCase(desc.getId()))
                .findFirst()
                .orElseGet(() -> {
                    if (failIfNotFound) {
                        throw new FcliSimpleException("Custom tag not found: " + customTagSpec);
                    }
                    return null;
                });
    }

    public Stream<SSCCustomTagDescriptor> getDescriptorsByCustomTagSpec(List<String> customTagSpecs, boolean failIfNotFound) {
        if (customTagSpecs == null || customTagSpecs.isEmpty()) {
            return Stream.empty();
        }
        return customTagSpecs.stream()
                .map(spec -> getDescriptorByCustomTagSpec(spec, failIfNotFound))
                .filter(Objects::nonNull)
                .distinct();
    }
}