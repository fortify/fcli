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
package com.fortify.cli.fod.report.helper;

import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.common.util.StringUtils;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.UnirestInstance;
import lombok.Getter;


public class FoDReportTemplateHelper {
    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    public final static String LOOKUP_TYPE = "ReportTemplateTypes";

    public static final JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final JsonNode filterTemplatesOnGroup(JsonNode record, FoDReportTemplateGroupType groupType) {
        JsonNode retval = null;
        if (groupType != null && record.has("group")) {
            String matchingGroupText = StringUtils.capitalize(groupType.name()) + " Report";
            if (groupType == FoDReportTemplateGroupType.All || record.findValuesAsText("group").contains(matchingGroupText)) {
                retval = record;
            }
        }
        return retval;
    }

    public static final FoDReportTemplateDescriptor getReportTemplateDescriptor(UnirestInstance unirest, String reportTemplateNameOrId, boolean failIfNotFound) {
        FoDReportTemplateDescriptor result = null;
        try {
            int templateId = Integer.parseInt(reportTemplateNameOrId);
            Optional<JsonNode> template = getReportTemplatesStream(unirest)
                    .filter(n -> n.get("value").asInt() == templateId)
                    .findFirst();
            result = (template.isEmpty() ? getEmptyDescriptor() : getDescriptor(template.get()));
        } catch (NumberFormatException nfe) {
            Optional<JsonNode> template = getReportTemplatesStream(unirest)
                    .filter(n -> n.get("text").asText().equals(reportTemplateNameOrId))
                    .findFirst();
            result = (template.isEmpty() ? getEmptyDescriptor() : getDescriptor(template.get()));        }
        if ( failIfNotFound && result==null ) {
            throw new IllegalArgumentException("No report template found for name or id: " + reportTemplateNameOrId);
        }
        return result;
    }

    private static Stream<JsonNode> getReportTemplatesStream(UnirestInstance unirest) {
        return JsonHelper.stream((ArrayNode)unirest.get(FoDUrls.LOOKUP_ITEMS)
                .queryString("type", LOOKUP_TYPE).asObject(ObjectNode.class).getBody().get("items"));
    }

    private static final FoDReportTemplateDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDReportTemplateDescriptor.class);
    }

    private static final FoDReportTemplateDescriptor getEmptyDescriptor() {
        return JsonHelper.treeToValue(objectMapper.createObjectNode(), FoDReportTemplateDescriptor.class);
    }

}
