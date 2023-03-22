package com.fortify.cli.ssc.appversion_artifact.helper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SSCAppVersionArtifactHelper {
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 1;
    
    private SSCAppVersionArtifactHelper() {}
    
    public static final SSCAppVersionArtifactDescriptor getArtifactDescriptor(UnirestInstance unirest, String artifactId) {
        return getDescriptor(getArtifactJsonNode(unirest, artifactId));
    }

    private static JsonNode getArtifactJsonNode(UnirestInstance unirest, String artifactId) {
        return unirest.get(SSCUrls.ARTIFACT(artifactId))
                .queryString("embed","scans")
                .asObject(JsonNode.class).getBody().get("data");
    }
    
    public static final SSCAppVersionArtifactDescriptor delete(UnirestInstance unirest, SSCAppVersionArtifactDescriptor descriptor) {
        unirest.delete(SSCUrls.ARTIFACT(descriptor.getId())).asObject(JsonNode.class).getBody();
        return descriptor;
    }
    
    public static final SSCAppVersionArtifactDescriptor purge(UnirestInstance unirest, SSCAppVersionArtifactDescriptor descriptor) {
        unirest.post(SSCUrls.ARTIFACTS_ACTION_PURGE)
            .body(new SSCAppVersionArtifactPurgeByIdRequest(new String[] {descriptor.getId()}))
            .asObject(JsonNode.class).getBody();
        return descriptor;
    }
    
    public static final JsonNode purge(UnirestInstance unirest, SSCAppVersionArtifactPurgeByDateRequest purgeRequest) {
        return unirest.post(SSCUrls.PROJECT_VERSIONS_ACTION_PURGE)
                .body(purgeRequest).asObject(JsonNode.class).getBody();
    }
    
    public static final JsonNode approve(UnirestInstance unirest, String artifactId, String message){
        int[] artifactIds = {Integer.parseInt(artifactId)};

        JsonNode jsonNode = new ObjectMapper().createObjectNode()
                .putPOJO("artifactIds", artifactIds)
                .put("comment", message);

        return unirest.post(SSCUrls.ARTIFACTS_ACTION_APPROVE)
                .body(jsonNode)
                .asObject(JsonNode.class).getBody();
    }
    
    public static final String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateSpELExpression(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "data.status",
                String.class
        );
    }
    
    @Data @ReflectiveAccess @AllArgsConstructor
    private static final class SSCAppVersionArtifactPurgeByIdRequest {
        private String[] artifactIds;
    }
    
    @Data @ReflectiveAccess @Builder @NoArgsConstructor @AllArgsConstructor
    public static final class SSCAppVersionArtifactPurgeByDateRequest {
        private String[] projectVersionIds;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSxxx") 
        private OffsetDateTime purgeBefore;
    }
    
    private static final SSCAppVersionArtifactDescriptor getDescriptor(JsonNode scanNode) {
        return JsonHelper.treeToValue(scanNode, SSCAppVersionArtifactDescriptor.class);
    }

    public static JsonNode addScanTypes(JsonNode record) {
        if ( record instanceof ObjectNode && record.has("_embed") ) {
            JsonNode _embed = record.get("_embed");
            String scanTypesString = "";
            if ( _embed.has("scans") ) {
                // TODO Can we get rid of unchecked conversion warning?
                ArrayList<String> scanTypes = JsonHelper.evaluateSpELExpression(_embed, "scans?.![type]", ArrayList.class);
                scanTypesString = scanTypes.stream().collect(Collectors.joining(", "));   
            }
            record = ((ObjectNode)record).put("scanTypes", scanTypesString);
        }
        return record;
    }
}
