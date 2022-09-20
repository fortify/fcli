package com.fortify.cli.ssc.appversion_artifact.helper;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.ssc.rest.SSCUrls;

import kong.unirest.UnirestInstance;

public final class SSCAppVersionArtifactHelper {
    public static final int DEFAULT_POLL_INTERVAL_SECONDS = 1;
    
    private SSCAppVersionArtifactHelper() {}
    
    public static final JsonNode delete(UnirestInstance unirest, String artifactId) {
        return unirest.delete(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody();
    }
    
    public static final String waitForNonProcessingState(UnirestInstance unirest, String artifactId, int pollIntervalSeconds, int timeOutSeconds) {
        Set<String> incompleteStates = new HashSet<>(Arrays.asList("PROCESSING", "SCHED_PROCESSING"));
        long startTime = new Date().getTime();
        String status = getArtifactStatus(unirest, artifactId);
        while (new Date().getTime() < timeOutSeconds*1000+startTime && incompleteStates.contains(status) ) {
            try {
                Thread.sleep(pollIntervalSeconds*1000);
                status = getArtifactStatus(unirest, artifactId);
            } catch (InterruptedException ignore) {}
        }
        if ( incompleteStates.contains(status) ) {
            throw new RuntimeException("Time-out while waiting for SSC to process the uploaded artifact");
        }
        return status;
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
    
    public static final String waitAndApprove(UnirestInstance unirest, String artifactId, String message, int pollIntervalSeconds, int timeOutSeconds) {
        // TODO This may actually wait for 2*timeOutSeconds; once before and once after approving
        String state = SSCAppVersionArtifactHelper.waitForNonProcessingState(unirest, artifactId, pollIntervalSeconds, timeOutSeconds);
        if ("REQUIRE_AUTH".equals(state)) {
            SSCAppVersionArtifactHelper.approve(unirest, artifactId, message);
            state = SSCAppVersionArtifactHelper.waitForNonProcessingState(unirest, artifactId, pollIntervalSeconds, timeOutSeconds);
        }
        return state;
    }
    
    public static final String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateJsonPath(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "$.data.status",
                String.class
        );
    }
}
