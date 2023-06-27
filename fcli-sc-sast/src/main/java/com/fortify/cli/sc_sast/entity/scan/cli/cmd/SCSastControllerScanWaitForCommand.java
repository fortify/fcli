/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.sc_sast.entity.scan.cli.cmd;

import java.util.function.BiFunction;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_sast.entity.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobArtifactState;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.entity.scan.helper.SCSastControllerScanJobState;
import com.fortify.cli.sc_sast.output.cli.mixin.SCSastControllerProductHelperMixin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SCSastControllerScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin SCSastControllerProductHelperMixin productHelper;
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
