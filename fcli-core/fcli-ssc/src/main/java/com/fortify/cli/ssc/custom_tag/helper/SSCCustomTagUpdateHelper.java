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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

public class SSCCustomTagUpdateHelper {
    private final SSCCustomTagHelper tagHelper;

    public SSCCustomTagUpdateHelper(UnirestInstance unirest) {
        this.tagHelper = new SSCCustomTagHelper(unirest);
    }

    /**
     * Resolves tag specs (name, guid, id) to descriptors using SSCCustomTagHelper.
     */
    public Set<SSCCustomTagDescriptor> resolveTagSpecs(List<String> tagSpecs) {
        return tagHelper.getDescriptorsByCustomTagSpec(tagSpecs, false).collect(Collectors.toSet());
    }

    /**
     * Computes the updated stream of custom tag descriptors given current, add, and remove specs.
     */
    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(List<SSCCustomTagDescriptor> currentTags, List<String> addSpecs, List<String> rmSpecs) {
        var currentTagsStream = currentTags.stream();
        var addDescriptorsStream = tagHelper.getDescriptorsByCustomTagSpec(addSpecs, false);
        var rmDescriptors = tagHelper.getDescriptorsByCustomTagSpec(rmSpecs, false).toList();
        return Stream.concat(
                currentTagsStream.filter(tag -> rmDescriptors.stream().noneMatch(rmTag -> rmTag.isEqualById(tag))),
                addDescriptorsStream
        ).distinct();
    }

    /**
     * Overload: Accepts current custom tags as json nodes, resolves to descriptors, then computes updated descriptors.
     */
    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(JsonNode currentTagsNode, List<String> addSpecs, List<String> rmSpecs) {
        return computeUpdatedTagDescriptors(
                SSCCustomTagHelper.toDescriptors(currentTagsNode),
                addSpecs, rmSpecs);
    }
}