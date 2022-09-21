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

import com.fortify.cli.common.output.cli.mixin.OutputConfig;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.rest.SSCUrls;
import com.fortify.cli.ssc.rest.cli.cmd.AbstractSSCTableOutputCommand;
import com.fortify.cli.ssc.util.SSCOutputConfigHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.GetRequest;
import kong.unirest.UnirestInstance;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@ReflectiveAccess
@Command(name = "approve")
public class SSCAppVersionArtifactApproveCommand extends AbstractSSCTableOutputCommand {
    private static final int POLL_INTERVAL_SECONDS = SSCAppVersionArtifactHelper.DEFAULT_POLL_INTERVAL_SECONDS;
    
    @Parameters(arity="1", description = "Id of the artifact to be approved")
    private String artifactId;
    
    @Option(names = {"-w", "--wait"}, defaultValue = "false", description = "Will wait for the artifact to finish processing before/after approving.")
    private Boolean wait;

    @Option(names = {"-t", "--time-out"}, defaultValue="300")
    private int processingTimeOutSeconds;
    
    @Option(names = {"-m", "--message"}, defaultValue = "Auto-approved by fcli")
    private String message;
    
    @Override
    protected GetRequest generateRequest(UnirestInstance unirest) {
        if ( wait ) {
            SSCAppVersionArtifactHelper.waitAndApprove(unirest, artifactId, message, POLL_INTERVAL_SECONDS, processingTimeOutSeconds);
        } else {
            SSCAppVersionArtifactHelper.approve(unirest, artifactId, message);
        }

        return unirest.get(SSCUrls.ARTIFACT(artifactId)).queryString("embed","scans");
    }
    
    @Override
    public OutputConfig getOutputOptionsWriterConfig() {
        return SSCOutputConfigHelper.tableFromData()
                .defaultColumns("id#$[*].scans[*].type:type#lastScanDate#uploadDate#status");
    }
}
