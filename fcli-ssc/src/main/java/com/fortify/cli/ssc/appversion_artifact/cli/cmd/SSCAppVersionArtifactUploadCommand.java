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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.common.util.JsonHelper;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionDescriptor;
import com.fortify.cli.ssc.appversion.cli.mixin.SSCAppVersionResolverMixin;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCTableOutputCommand;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper;
import com.fortify.cli.ssc.rest.transfer.SSCFileTransferHelper.ISSCAddUploadTokenFunction;
import com.fortify.cli.ssc.util.SSCOutputHelper;
import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@ReflectiveAccess
@Command(name = "upload")
public class SSCAppVersionArtifactUploadCommand extends AbstractSSCTableOutputCommand {
    @CommandLine.Mixin private SSCAppVersionResolverMixin.To parentVersionHandler;
    @CommandLine.Option(names = {"-f", "--file"}, required = true)
    private String filePath;

    @CommandLine.Option(names = {"-a", "--auto-approve"}, defaultValue = "false", description = "Auto approves any uploaded artifact that needs approval.")
    private Boolean autoApprove;

    @CommandLine.Option(names = {"-w", "--wait"}, defaultValue = "false", description = "Will wait for the artifact to finish processing and auto approve if needed.")
    private Boolean wait;

    // Timeout in seconds
    private int initialStatusTimeOutSeconds = 5;
    private int processingTimeOutSeconds = 300;

    private void doApprove(UnirestInstance unirest, String artifactId){
        int[] artifactIds = {Integer.parseInt(artifactId)};

        JsonNode jsonNode = (new ObjectMapper()).createObjectNode()
                .putPOJO("artifactIds", artifactIds)
                .put("comment","Auto approved via fcli.");

        unirest.post(SSCUrls.ARTIFACTS_ACTION_APPROVE)
                .body(jsonNode)
                .asObject(JsonNode.class);
    }


    private void doWait(UnirestInstance unirest, String artifactId, int timeOutSeconds){
        String status = waitForNonProcessingStatus(unirest, artifactId, timeOutSeconds);

        if("REQUIRE_AUTH".equals(status)){
            doApprove(unirest, artifactId);
            doWait(unirest, artifactId, processingTimeOutSeconds);
        }else if("PROCESS_COMPLETE".equals(status)){
            // Do nothing!
            return;
        }else if("PROCESSING".equals(status) || "SCHED_PROCESSING".equals(status)){
            throw new RuntimeException("Fcli timed out when waiting for the artifact to finish processing. Last known state: " + status);
        }else{
            throw new RuntimeException("Fcli tried uploading the artifact, but encountered a unhandled state: " + status);
        }
    }

    private String getArtifactStatus(UnirestInstance unirest, String artifactId){
        return JsonHelper.evaluateJsonPath(
                unirest.get(SSCUrls.ARTIFACT(artifactId)).asObject(JsonNode.class).getBody(),
                "$.data.status",
                String.class
        );
    }


    public String waitForNonProcessingStatus(UnirestInstance unirest, String artifactId, int timeOutSeconds) {
        Set<String> incompleteStates = new HashSet<>(Arrays.asList("PROCESSING", "SCHED_PROCESSING"));
        long startTime = new Date().getTime();
        String status = getArtifactStatus(unirest, artifactId);
        while (new Date().getTime() < startTime+timeOutSeconds*1000 && incompleteStates.contains(status) ) {
            try {
                Thread.sleep(1000L);
                status = getArtifactStatus(unirest, artifactId);
            } catch (InterruptedException ignore) {}
        }
        return status;
    }

    @Override
    protected GetRequest generateRequest(UnirestInstance unirest) {
        SSCAppVersionDescriptor av = parentVersionHandler.getApplicationAndVersion(unirest);
        JsonNode uploadResponse = SSCFileTransferHelper.upload(
                unirest,
                SSCUrls.PROJECT_VERSION_ARTIFACTS(av.getApplicationVersionId()),
                filePath,
                ISSCAddUploadTokenFunction.AUTHHEADER,
                JsonNode.class
        );
        String artifactId = JsonHelper.evaluateJsonPath(uploadResponse, "$.data.id", String.class);

        if(wait){
            doWait(unirest, artifactId, initialStatusTimeOutSeconds);
        }else if(autoApprove){
            doApprove(unirest, artifactId);
        }

        return unirest.get(SSCUrls.ARTIFACT(artifactId)).queryString("embed","scans");
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputHelper.defaultTableOutputConfig()
                .defaultColumns("id#$[*].scans[*].type:type#lastScanDate#uploadDate#status");
    }
}
