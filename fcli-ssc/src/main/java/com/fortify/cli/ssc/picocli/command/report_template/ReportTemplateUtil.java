package com.fortify.cli.ssc.picocli.command.report_template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc.common.SSCUrls;
import com.fortify.cli.ssc.common.pojos.reportTemplateDef.existingReportTemplate.ReportTemplateDef;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;

public class ReportTemplateUtil {
    @SneakyThrows
    public static ReportTemplateDef fetchReportDefInfo(UnirestInstance unirestInstance, String reportTemplateNameOrId, boolean isReportName){
        ReportTemplateDef returnObj;
        ObjectMapper om = new ObjectMapper();
        HttpResponse response;
        boolean isNumeric = true;
        String id = "-1";

        try{
            id = Integer.toString(Integer.parseInt(reportTemplateNameOrId));
        }catch (NumberFormatException e){
            isNumeric = false;
        }

        if(!isNumeric || isReportName){
            response = unirestInstance.get(SSCUrls.REPORT_DEFINITIONS)
                    .queryString("limit","1")
                    .queryString("fields","id")
                    .queryString("q", String.format("name:%s", reportTemplateNameOrId))
                    .asObject(ObjectNode.class);
            id = JsonPath.parse(response.getBody().toString()).read("$.data[0].id").toString();
        }
        response = unirestInstance.get(SSCUrls.REPORT_DEFINITION(id))
                .queryString("fields","id,name,fileName")
                .asObject(ObjectNode.class);

        returnObj = om.readValue(response.getBody().toString(), ReportTemplateDef.class);
        return returnObj;
    }
}
