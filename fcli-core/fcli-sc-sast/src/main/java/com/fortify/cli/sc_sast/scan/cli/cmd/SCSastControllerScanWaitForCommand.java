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
package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_sast._common.output.cli.mixin.SCSastControllerProductHelperStandardMixin;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobArtifactState.SCSastControllerScanJobArtifactStateIterable;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobState;
import com.fortify.cli.sc_sast.scan.helper.SCSastControllerScanJobState.SCSastControllerScanJobStateIterable;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SCSastControllerScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin SCSastControllerProductHelperStandardMixin productHelper;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver;
    @ArgGroup(exclusive = true, multiplicity = "0..1") private WaitOptions waitOptions;
    private static final class WaitOptions {
        @Option(names={"--any-scan-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobStateIterable.class)
        private Set<String> scanStates;
        @Option(names={"--any-upload-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobStateIterable.class)
        private Set<String> uploadStates;
        @Option(names={"--any-ssc-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobArtifactStateIterable.class)
        private Set<String> sscStates;
    }
    
    @Override
    protected WaitHelperBuilder configure(WaitHelperBuilder builder) {
        Set<String> sscStates = waitOptions==null 
                ? Set.of(SCSastControllerScanJobArtifactState.getDefaultCompleteStateNames()) 
                : waitOptions.sscStates;
        if ( sscStates!=null && !sscStates.isEmpty() ) {
            return builder
                .recordsSupplier(u->scanJobsResolver.getScanJobDescriptorJsonNodes(u, StatusEndpointVersion.v3))
                .currentStateProperty("sscArtifactState")
                .knownStates(SCSastControllerScanJobArtifactState.getKnownStateNames())
                .failureStates(SCSastControllerScanJobArtifactState.getFailureStateNames())
                .matchStates(sscStates);
        } else if ( waitOptions.uploadStates!=null && !waitOptions.uploadStates.isEmpty() ) {
            return builder
                .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                .currentStateProperty("sscUploadState")
                .knownStates(SCSastControllerScanJobState.getKnownStateNames())
                .failureStates(SCSastControllerScanJobState.getFailureStateNames())
                .matchStates(waitOptions.uploadStates);
        } else if ( waitOptions.scanStates!=null && !waitOptions.scanStates.isEmpty() ) {
            return builder
                    .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                    .currentStateProperty("scanState")
                    .knownStates(SCSastControllerScanJobState.getKnownStateNames())
                    .failureStates(SCSastControllerScanJobState.getFailureStateNames())
                    .matchStates(waitOptions.scanStates);
        } else {
            throw new RuntimeException("Unexpected situation, please file a bug");
        }
    }
}
