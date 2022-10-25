package com.fortify.cli.sc_sast.scan.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;

import kong.unirest.UnirestInstance;

public class SCSastControllerScanJobHelper {
    private static final String[] statusEndpoints = {"/rest/v3/job/{token}/status", "/rest/v2/job/{token}/status", "/rest/job/{token}/status"};
    
    public static final SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, String scanJobToken) {
        SCSastControllerScanJobDescriptor descriptor = null;
        RuntimeException lastException = null;
        
        for ( String endpoint : statusEndpoints ) {
            try {
                descriptor = getScanJobDescriptor(
                        unirest.get(endpoint).routeParam("token", scanJobToken)
                            .asObject(ObjectNode.class).getBody()
                            .put("jobToken", scanJobToken));
                break;
            } catch ( RuntimeException e ) {
                lastException = e;
            }
        }
        
        if ( descriptor==null ) {
            throw new RuntimeException("Error getting job status from ScanCentral SAST Controller", lastException);
        }
        return descriptor;
    }

    private static SCSastControllerScanJobDescriptor getScanJobDescriptor(ObjectNode node) {
        return JsonHelper.treeToValue(node, SCSastControllerScanJobDescriptor.class);
    }
}
