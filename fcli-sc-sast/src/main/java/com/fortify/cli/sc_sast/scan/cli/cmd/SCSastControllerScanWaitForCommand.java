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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.util.function.BiFunction;

import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerBasicOutputHelperMixins;
import com.fortify.cli.sc_sast.rest.cli.mixin.SCSastControllerUnirestRunnerMixin;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobState;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = SCSastControllerBasicOutputHelperMixins.WaitFor.CMD_NAME)
public class SCSastControllerScanWaitForCommand extends AbstractWaitForCommand<SCSastSessionData> {
    @Getter @Mixin SCSastControllerUnirestRunnerMixin unirestRunner;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver;
    @Option(names={"-s", "--status-type"}, defaultValue="processing", required=true) private WaitType waitType;
    
    @Override
    protected WaitHelperBuilder configure(WaitHelperBuilder builder) {
        return waitType.configurer.apply(builder, scanJobsResolver);
    }
    
    @RequiredArgsConstructor
    private static enum WaitType {
        scan(WaitType::configureWaitForScan), 
        upload(WaitType::configureWaitForUpload), 
        processing(WaitType::configureWaitForProcessing);
        
        private final BiFunction<WaitHelperBuilder, SCSastScanJobResolverMixin.PositionalParameterMulti, WaitHelperBuilder> configurer;
        
        private static WaitHelperBuilder configureWaitForScan(WaitHelperBuilder builder, SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver) {
            return builder
                    .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                    .currentStateProperty("scanState")
                    .knownStates(SCSastControllerScanJobState.getKnownStateNames())
                    .failureStates(SCSastControllerScanJobState.getFailureStateNames())
                    .defaultCompleteStates(SCSastControllerScanJobState.getDefaultCompleteStateNames());
        }
        
        private static WaitHelperBuilder configureWaitForUpload(WaitHelperBuilder builder, SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver) {
            return builder
                    .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                    .currentStateProperty("sscUploadState")
                    .knownStates(SCSastControllerScanJobState.getKnownStateNames())
                    .failureStates(SCSastControllerScanJobState.getFailureStateNames())
                    .defaultCompleteStates(SCSastControllerScanJobState.getDefaultCompleteStateNames());
        }
        
        private static WaitHelperBuilder configureWaitForProcessing(WaitHelperBuilder builder, SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver) {
            return builder
                    .recordsSupplier(u->scanJobsResolver.getScanJobDescriptorJsonNodes(u, StatusEndpointVersion.v3))
                    .currentStateProperty("sscArtifactState")
                    .knownStates(SCSastControllerScanJobArtifactState.getKnownStateNames())
                    .failureStates(SCSastControllerScanJobArtifactState.getFailureStateNames())
                    .defaultCompleteStates(SCSastControllerScanJobArtifactState.getDefaultCompleteStateNames());
        }
    }
}
