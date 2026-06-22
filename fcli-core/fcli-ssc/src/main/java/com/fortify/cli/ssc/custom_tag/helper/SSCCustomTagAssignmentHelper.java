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
package com.fortify.cli.ssc.custom_tag.helper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import kong.unirest.UnirestInstance;

public class SSCCustomTagAssignmentHelper {
    private final SSCCustomTagDefinitionHelper tagDefinitionHelper;

    public SSCCustomTagAssignmentHelper(UnirestInstance unirest) {
        this.tagDefinitionHelper = new SSCCustomTagDefinitionHelper(unirest);
    }

    public Set<SSCCustomTagDescriptor> resolveTagSpecs(List<String> tagSpecs) {
        return tagDefinitionHelper.getDescriptorsByCustomTagSpec(tagSpecs, false).collect(Collectors.toSet());
    }

    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(List<SSCCustomTagDescriptor> currentTags, List<String> addSpecs, List<String> rmSpecs) {
        var currentTagsStream = currentTags.stream();
        var addDescriptorsStream = tagDefinitionHelper.getDescriptorsByCustomTagSpec(addSpecs, false);
        var rmDescriptors = tagDefinitionHelper.getDescriptorsByCustomTagSpec(rmSpecs, false).toList();
        return Stream.concat(
                currentTagsStream.filter(tag -> rmDescriptors.stream().noneMatch(rmTag -> rmTag.isEqualById(tag))),
                addDescriptorsStream
        ).distinct();
    }

    public Stream<SSCCustomTagDescriptor> computeUpdatedTagDescriptors(JsonNode currentTagsNode, List<String> addSpecs, List<String> rmSpecs) {
        return computeUpdatedTagDescriptors(
                SSCCustomTagDefinitionHelper.toDescriptors(currentTagsNode),
                addSpecs, rmSpecs);
    }
}