/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
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
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCTableOutputCommand;
import com.fortify.cli.ssc.util.SSCOutputHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.UnirestInstance;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "upload")
public class SSCAppVersionArtifactUploadCommand extends AbstractSSCTableOutputCommand {
    private static final long SLEEP_TIME = 1000L;
    @CommandLine.Mixin private SSCAppVersionResolverMixin.To parentVersionHandler;
    @Parameters(arity="1")
    private String filePath;

    @CommandLine.Option(names = {"-a", "--auto-approve"}, defaultValue = "false", description = "Auto approves any uploaded artifact that needs approval.")
    private Boolean autoApprove;

    @CommandLine.Option(names = {"-w", "--wait"}, defaultValue = "false", description = "Will wait for the artifact to finish processing and auto approve if needed.")
    private Boolean wait;
    
    @CommandLine.Option(names = {"-e", "--engine-type"}, description = "Engine type for the artifact being uploaded")
    private String engineType;

    // Timeout in seconds
    private int processingTimeOutSeconds = 300;
    
    @Override
    protected GetRequest generateRequest(UnirestInstance unirest) {
        SSCAppVersionDescriptor av = parentVersionHandler.getApplicationAndVersion(unirest);
        HttpRequestWithBody request = unirest.post(SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getApplicationVersionId()));
        if ( engineType!=null && !engineType.isBlank() ) {
            request.queryString("engineType", engineType);
        }
        JsonNode uploadResponse = request.multiPartContent()
                .field("file", new File(filePath))
                .asObject(JsonNode.class).getBody();
        
        String artifactId = JsonHelper.evaluateJsonPath(uploadResponse, "$.data.id", String.class);
        
        String state = null;
        if (wait || autoApprove) {
            state = waitForNonProcessingState(unirest, artifactId);
        }
        if (autoApprove && "REQUIRE_AUTH".equals(state)) {
            approve(unirest, artifactId);
            waitForNonProcessingState(unirest, artifactId);
        }

        return unirest.get(SSCUrls.ARTIFACT(artifactId)).queryString("embed","scans");
    }

    public String waitForNonProcessingState(UnirestInstance unirest, String artifactId) {
        Set<String> incompleteStates = new HashSet<>(Arrays.asList("PROCESSING", "SCHED_PROCESSING"));
        long startTime = new Date().getTime();
        String status = getArtifactStatus(unirest, artifactId);
        while (new Date().getTime() < processingTimeOutSeconds*1000+startTime && incompleteStates.contains(status) ) {
            try {
                Thread.sleep(SLEEP_TIME);
                status = getArtifactStatus(unirest, artifactId);
            } catch (InterruptedException ignore) {}
        }
        if ( incompleteStates.contains(status) ) {
            throw new RuntimeException("Time-out while waiting for SSC to process the uploaded artifact");
        }
        return status;
    }
    
    private void approve(UnirestInstance unirest, String artifactId){
        int[] artifactIds = {Integer.parseInt(artifactId)};

        JsonNode jsonNode = new ObjectMapper().createObjectNode()
                .putPOJO("artifactIds", artifactIds)
                .put("comment","Auto approved via fcli.");

        unirest.post(SSCUrls.ARTIFACTS_ACTION_APPROVE)
                .body(jsonNode)
                .asObject(JsonNode.class);
    }
    
    private String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateJsonPath(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "$.data.status",
                String.class
        );
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
                .defaultColumns("id#$[*].scans[*].type:type#lastScanDate#uploadDate#status");
    }
}
