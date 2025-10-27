/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.sc_sast.scan.cli.cmd;

import java.util.Set;
import java.util.stream.Stream;

import com.fortify.cli.common.exception.FcliBugException;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_sast.scan.cli.mixin.SCSastScanJobResolverMixin;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobArtifactState;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobArtifactState.SCSastControllerScanJobArtifactStateIterable;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobDescriptor;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobHelper.StatusEndpointVersion;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobState;
import com.fortify.cli.sc_sast.scan.helper.SCSastScanJobState.SCSastControllerScanJobStateIterable;
import com.fortify.cli.ssc._common.rest.sc_sast.cli.mixin.SCSastUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SCSastScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin private SCSastUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private SCSastScanJobResolverMixin.PositionalParameterMulti scanJobsResolver;
    @ArgGroup(exclusive = true, multiplicity = "0..1") private WaitOptions waitOptions;
    private static final class WaitOptions {
        @Option(names={"--any-scan-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobStateIterable.class)
        private Set<String> scanStates;
        @Option(names={"--any-publish-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobStateIterable.class)
        private Set<String> publishStates;
        @Option(names={"--any-ssc-state"}, required=true, split=",", completionCandidates = SCSastControllerScanJobArtifactStateIterable.class)
        private Set<String> sscStates;
        
        public boolean isEmpty() {
            return (scanStates==null || scanStates.isEmpty())
                    && (publishStates==null || publishStates.isEmpty())
                    && (sscStates==null || sscStates.isEmpty())
                    ;
        }
    }
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        Set<String> scanStates = null;
        Set<String> publishStates  = null;
        Set<String> sscStates  = null;
        if ( waitOptions!=null && !waitOptions.isEmpty() ) {
            scanStates = waitOptions.scanStates;
            publishStates = waitOptions.publishStates;
            sscStates = waitOptions.sscStates;
        } else {
            SCSastScanJobDescriptor[] scanJobDescriptors = scanJobsResolver.getScanJobDescriptors(unirest);
            var allPublishRequested = Stream.of(scanJobDescriptors)
                    .allMatch(SCSastScanJobDescriptor::isPublishRequested);
            var v3Endpoints = Stream.of(scanJobDescriptors)
                    .anyMatch(d->d.getEndpointVersion()>=3);
            if ( allPublishRequested && v3Endpoints ) {
                sscStates = Set.of(SCSastScanJobArtifactState.getDefaultCompleteStateNames());
            } else if ( allPublishRequested ) {
                publishStates = Set.of(SCSastScanJobState.getDefaultCompleteStateNames());
            } else {
                scanStates = Set.of(SCSastScanJobState.getDefaultCompleteStateNames());
            }
        }
        if ( sscStates!=null ) {
            return builder
                .recordsSupplier(u->scanJobsResolver.getScanJobDescriptorJsonNodes(u, StatusEndpointVersion.v3))
                .currentStateProperty("sscArtifactState")
                .knownStates(SCSastScanJobArtifactState.getKnownStateNames())
                .failureStates(SCSastScanJobArtifactState.getFailureStateNames())
                .matchStates(sscStates);
        } else if ( publishStates!=null ) {
            return builder
                .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                .currentStateProperty("publishState")
                .knownStates(SCSastScanJobState.getKnownStateNames())
                .failureStates(SCSastScanJobState.getFailureStateNames())
                .matchStates(publishStates);
        } else if ( scanStates!=null ) {
            return builder
                    .recordsSupplier(scanJobsResolver::getScanJobDescriptorJsonNodes)
                    .currentStateProperty("scanState")
                    .knownStates(SCSastScanJobState.getKnownStateNames())
                    .failureStates(SCSastScanJobState.getFailureStateNames())
                    .matchStates(scanStates);
        } else {
            throw new FcliBugException("Unexpected situation, please file a bug");
        }
    }
}
