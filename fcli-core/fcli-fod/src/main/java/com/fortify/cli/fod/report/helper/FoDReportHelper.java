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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.fod._common.rest.FoDUrls;
import kong.unirest.UnirestInstance;
import lombok.Getter;

public class FoDReportHelper {
    @Getter
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static final JsonNode transformRecord(JsonNode record) {
        return new RenameFieldsTransformer(new String[]{}).transform(record);
    }

    public static final FoDReportDescriptor createReport(UnirestInstance unirest, FoDReportCreateRequest reportCreateRequest) {
        ObjectNode body = objectMapper.valueToTree(reportCreateRequest);
        var reportId = unirest.post(FoDUrls.REPORTS)
                .body(body).asObject(JsonNode.class).getBody().get("reportId").asText();
        return getReportDescriptor(unirest, reportId);
    }

    public static final FoDReportDescriptor getReportDescriptor(UnirestInstance unirest, String reportId) {
        var result = unirest.get(FoDUrls.REPORT)
                .routeParam("reportId", reportId)
                .asObject(ObjectNode.class)
                .getBody();
        return getDescriptor(result);
    }

    private static final FoDReportDescriptor getDescriptor(JsonNode node) {
        return JsonHelper.treeToValue(node, FoDReportDescriptor.class);
    }

}
