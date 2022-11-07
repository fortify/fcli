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

package com.fortify.cli.fod.dast_scan.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod.release.helper.FoDAppRelAssessmentTypeDescriptor;
import com.fortify.cli.fod.release.helper.FoDAppRelHelper;
import com.fortify.cli.fod.rest.FoDUrls;
import com.fortify.cli.fod.scan.helper.FoDAssessmentTypeDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDAssessmentTypeOptions.FoDAssessmentType;
import com.fortify.cli.fod.scan.helper.FoDScanDescriptor;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanTypeOptions;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.util.FoDEnums;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import lombok.Getter;

import javax.validation.ValidationException;

public class FoDDastScanHelper extends FoDScanHelper {
    @Getter private static ObjectMapper objectMapper = new ObjectMapper();

    /*public static final FoDDastScanSetupDescriptor setupScan(UnirestInstance unirest, Integer relId, FoDSetupDastScanRequest setupDastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(setupDastScanRequest);
        FoDQueryHelper.stripNulls(body);
        System.out.println(body.toPrettyString());
        JsonNode response = unirest.put(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        return JsonHelper.treeToValue(response, FoDDastScanSetupDescriptor.class);
    }*/

    public static final FoDScanDescriptor startScan(UnirestInstance unirest, Integer relId, FoDStartDastScanRequest startDastScanRequest) {
        ObjectNode body = objectMapper.valueToTree(startDastScanRequest);
        JsonNode response = unirest.post(FoDUrls.DYNAMIC_SCANS + "/start-scan")
                .routeParam("relId", String.valueOf(relId))
                .body(body).asObject(JsonNode.class).getBody();
        FoDScanDescriptor descriptor = JsonHelper.treeToValue(response, FoDScanDescriptor.class);
        // TODO: wait until scan is being "executed" otherwise a 404
        return getScanDescriptor(unirest, String.valueOf(descriptor.getScanId()));
    }

    public static final FoDDastScanSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        GetRequest request = unirest.get(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", relId);
        JsonNode setup = request.asObject(ObjectNode.class).getBody();
        return JsonHelper.treeToValue(setup, FoDDastScanSetupDescriptor.class);
    }

}
