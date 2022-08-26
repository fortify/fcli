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
package com.fortify.cli.ssc.report_template.cli.mixin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.ssc.report_template.domain.SSCReportTemplateDefResponse;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.jayway.jsonpath.JsonPath;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.HttpResponse;
import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;
import picocli.CommandLine;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
public class SSCReportTemplateIdMixin {
    @Parameters(index = "0", arity = "1", descriptionKey = "reportTemplateNameOrId")
    private String reportTemplateNameOrId;

    @CommandLine.Option(names = {"--isReportName"}, arity = "0", descriptionKey = "isReportName")
    private boolean isReportName = false;

    @SneakyThrows
    private SSCReportTemplateDefResponse fetchReportDefInfo(UnirestInstance unirestInstance, String reportTemplateNameOrId, boolean isReportName){
        SSCReportTemplateDefResponse returnObj;
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

        returnObj = om.readValue(response.getBody().toString(), SSCReportTemplateDefResponse.class);
        return returnObj;
    }

    public String getReportTemplateDefId(UnirestInstance unirestInstance) {
        return Integer.toString(fetchReportDefInfo(unirestInstance, reportTemplateNameOrId, isReportName).data.id);
    }
    public SSCReportTemplateDefResponse getReportTemplateDef(UnirestInstance unirestInstance){
        return fetchReportDefInfo(unirestInstance, reportTemplateNameOrId, isReportName);
    }
}
