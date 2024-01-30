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
package com.fortify.cli.ssc.report.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;

import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;

public final class SSCReportHelper {
    private static final RenameFieldsTransformer TRANSFORMER = new RenameFieldsTransformer("projects", "applications");

    /**
     * This method renames the following fields:
     * <ul>
     *  <li>projects => applications</li>
     *  <li>_embed.reportDefinition =&gt; template</li>
     * </ul>
     */
    public static final JsonNode renameFields(JsonNode record) {
        var obj = (ObjectNode)TRANSFORMER.transform(record);
        obj.set("template", obj.get("_embed").get("reportDefinition"));
        obj.remove("_embed");
        return obj;
    }
    
    public static final SSCReportDescriptor getRequiredReportDescriptor(UnirestInstance unirest, String reportNameOrId) {
        SSCReportDescriptor descriptor = getOptionalReportDescriptor(unirest, reportNameOrId);
        if ( descriptor==null ) {
            throw new IllegalArgumentException("No report found for name or id: "+reportNameOrId);
        }
        return descriptor;
    }
    
    public static final SSCReportDescriptor getOptionalReportDescriptor(UnirestInstance unirest, String reportNameOrId) {
        try {
            int reportId = Integer.parseInt(reportNameOrId);
            return getOptionalReportFromId(unirest, reportId);
        } catch (NumberFormatException nfe) {
            return getOptionalReportFromName(unirest, reportNameOrId);
        }
    }
    
    public static final SSCReportDescriptor getOptionalReportFromId(UnirestInstance unirest, int reportId) {
        GetRequest request = getBaseRequest(unirest).queryString("q", String.format("id:%d", reportId));
        return getOptionalDescriptor(request);
    }
    
    public static final SSCReportDescriptor getOptionalReportFromName(UnirestInstance unirest, String name) {
        GetRequest request = getBaseRequest(unirest);
        request = request.queryString("q", String.format("name:\"%s\"", name));
        return getOptionalDescriptor(request);
    }

    private static GetRequest getBaseRequest(UnirestInstance unirest) {
        return unirest.get("/api/v1/reports?limit=2")
                .queryString("embed", "reportDefinition");
    }

    private static final SSCReportDescriptor getOptionalDescriptor(GetRequest request) {
        JsonNode reports = request.asObject(ObjectNode.class).getBody().get("data");
        if ( reports.size()>1 ) {
            throw new IllegalArgumentException("Multiple reports found, please specify id");
        }
        return reports.size()==0 ? null : JsonHelper.treeToValue(renameFields(reports.get(0)), SSCReportDescriptor.class);
    }
}
