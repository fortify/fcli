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
package com.fortify.cli.sc_sast.scan.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class SCSastControllerScanJobHelper {
    @RequiredArgsConstructor
    public static enum StatusEndpointVersion {
        v3(3, "/rest/v3/job/{token}/status"), 
        v2(2, "/rest/v2/job/{token}/status"),
        v1(1, "/rest/job/{token}/status");
        
        @Getter private final int version;
        @Getter private final String endpoint;
    }
    
    public static final ObjectNode renameFields(ObjectNode node) {
        node = (ObjectNode)new RenameFieldsTransformer("state", "scanState").transform(node);
        node = (ObjectNode)new RenameFieldsTransformer("sscUploadState", "publishState").transform(node);
        return node;
    }
    
    public static final SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, String scanJobToken, StatusEndpointVersion minEndpointVersion) {
        SCSastControllerScanJobDescriptor descriptor = null;
        RuntimeException lastException = null;
        
        for ( StatusEndpointVersion endpointVersion : StatusEndpointVersion.values() ) {
            if ( minEndpointVersion==null || minEndpointVersion.getVersion()<=endpointVersion.getVersion() ) {
                try {
                    descriptor = getScanJobDescriptor(
                            unirest.get(endpointVersion.getEndpoint())
                                .routeParam("token", scanJobToken)
                                .asObject(ObjectNode.class).getBody()
                                .put("jobToken", scanJobToken)
                                .put("endpointVersion", endpointVersion.getVersion()));
                    break;
                } catch ( RuntimeException e ) {
                    lastException = e;
                }
            }
        }
        
        if ( descriptor==null ) {
            throw new RuntimeException("Error getting job status from ScanCentral SAST Controller", lastException);
        }
        return descriptor;
    }

    private static SCSastControllerScanJobDescriptor getScanJobDescriptor(ObjectNode node) {
        node = renameFields(node);
        if ( node.get("publishState").isNull() ) {
            node.put("publishState", SCSastControllerScanJobState.NO_PUBLISH.name());
            node.put("publishRequested", false);
        } else {
            node.put("publishRequested", true);
        }
        if ( node.get("sscArtifactState").isNull() ) {
            node.put("sscArtifactState", node.get("publishState").asText());
        }
        return JsonHelper.treeToValue(node, SCSastControllerScanJobDescriptor.class);
    }
}
