package com.fortify.cli.ssc.appversion.helper;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc._common.rest.ssc.SSCUrls;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagDescriptor;
import com.fortify.cli.ssc.custom_tag.helper.SSCCustomTagUpdateHelper;

import kong.unirest.HttpRequest;
import kong.unirest.UnirestInstance;
import lombok.RequiredArgsConstructor;

/**
 * Helper for updating custom tags on an SSC application version.
 * Accepts lists of custom tag nameOrGuid strings for additions/removals.
 */
@RequiredArgsConstructor
public class SSCAppVersionCustomTagUpdater {
    private final UnirestInstance unirest;

    public HttpRequest<?> buildRequest(String appVersionId, List<String> addCustomTagNameOrGuidList, List<String> rmCustomTagNameOrGuidList) {
        if ((addCustomTagNameOrGuidList == null || addCustomTagNameOrGuidList.isEmpty()) && (rmCustomTagNameOrGuidList == null || rmCustomTagNameOrGuidList.isEmpty())) {
            return null;
        }
        ArrayNode current = getInitialTags(appVersionId);
        SSCCustomTagUpdateHelper tagUpdateHelper = new SSCCustomTagUpdateHelper(unirest);
        List<SSCCustomTagDescriptor> currentDescriptors = JsonHelper.stream(current)
            .map(tag -> JsonHelper.treeToValue(tag, SSCCustomTagDescriptor.class))
            .collect(Collectors.toList());
        Set<String> updatedGuids = tagUpdateHelper.computeUpdatedTagDescriptors(
            currentDescriptors, addCustomTagNameOrGuidList, rmCustomTagNameOrGuidList
        ).map(SSCCustomTagDescriptor::getGuid).collect(Collectors.toSet());
        ArrayNode updated = new ObjectMapper().createArrayNode();
        updatedGuids.forEach(guid -> updated.add(objectNodeForGuid(guid)));
        if (arraysEqual(current, updated)) {
            return null;
        }
        return unirest.put(SSCUrls.PROJECT_VERSION_CUSTOM_TAGS(appVersionId)).body(updated);
    }

    private ArrayNode getInitialTags(String appVersionId) {
        return (ArrayNode) SSCAppVersionHelper.getCustomTagsRequest(unirest, appVersionId)
                .asObject(JsonNode.class).getBody().get("data");
    }

    private ObjectNode objectNodeForGuid(String guid) {
        return JsonHelper.getObjectMapper().createObjectNode().put("guid", guid);
    }

    private boolean arraysEqual(ArrayNode a, ArrayNode b) {
        if (a.size() != b.size()) {
            return false;
        }
        Set<String> aGuids = JsonHelper.stream(a).map(n -> n.get("guid").asText()).collect(Collectors.toSet());
        Set<String> bGuids = JsonHelper.stream(b).map(n -> n.get("guid").asText()).collect(Collectors.toSet());
        return aGuids.equals(bGuids);
    }
}