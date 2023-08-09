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

package com.fortify.cli.fod.scan_setup.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.fod._common.rest.FoDUrls;

import kong.unirest.UnirestInstance;

public class FoDScanDastSetupHelper {
    public static final FoDScanDastSetupDescriptor getSetupDescriptor(UnirestInstance unirest, String relId) {
        var body = unirest.get(FoDUrls.DYNAMIC_SCANS + "/scan-setup")
                .routeParam("relId", relId)
                .asObject(ObjectNode.class)
                .getBody();
        return JsonHelper.treeToValue(body, FoDScanDastSetupDescriptor.class);
    }
}
