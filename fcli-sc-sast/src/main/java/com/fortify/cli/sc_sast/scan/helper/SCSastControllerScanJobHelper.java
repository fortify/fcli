package com.fortify.cli.sc_sast.scan.helper;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.transform.fields.RenameFieldsTransformer;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactStatus;

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
    
    public static final SCSastControllerScanJobDescriptor getScanJobDescriptor(UnirestInstance unirest, String scanJobToken, Integer minEndpointVersion) {
        SCSastControllerScanJobDescriptor descriptor = null;
        RuntimeException lastException = null;
        
        for ( StatusEndpointVersion endpointVersion : StatusEndpointVersion.values() ) {
            if ( minEndpointVersion==null || minEndpointVersion<=endpointVersion.getVersion() ) {
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
        node = (ObjectNode)new RenameFieldsTransformer("state", "scanState").transform(node);
        if ( !node.get("sscUploadState").isNull() && node.get("sscArtifactState").isNull() ) {
            // Add non-null artifact state if upload was requested
            node = node.put("sscArtifactState", SSCAppVersionArtifactStatus.SCHED_PROCESSING.name());
        }
        
        return JsonHelper.treeToValue(node, SCSastControllerScanJobDescriptor.class);
    }
}
