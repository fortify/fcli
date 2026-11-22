/*
 * Copyright 2021-2026 Open Text.
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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_dast.scan.cli.mixin.SCDastScanResolverMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.ssc._common.rest.sc_dast.cli.mixin.SCDastUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SCDastScanWaitForCommand extends AbstractWaitForCommand {
    // Not an SC-DAST scan status; reported for completed scans if this state is requested and scan results have been published to SSC
    private static final String STATE_PUBLISHED = "Published";
    @Getter @Mixin SCDastUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private SCDastScanResolverMixin.PositionalParameterMulti scansResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="Complete", completionCandidates = SCDastScanWaitForStateIterable.class)
    private Set<String> states;

    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(SCDastScanStatus::addScanStatus)
                .currentState(this::getCurrentState)
                .knownStates(getKnownStateNames())
                .failureStates(SCDastScanStatus.getFailureStateNames())
                .matchStates(states);
    }

    private String getCurrentState(JsonNode record) {
        var scanStatus = record.path("scanStatus").asText();
        return states.contains(STATE_PUBLISHED)
                && SCDastScanStatus.Complete.name().equals(scanStatus)
                && STATE_PUBLISHED.equals(record.path("publishStatusTypeDescription").asText())
                ? STATE_PUBLISHED
                : scanStatus;
    }

    private static String[] getKnownStateNames() {
        return Stream.concat(Stream.of(SCDastScanStatus.getKnownStateNames()), Stream.of(STATE_PUBLISHED)).toArray(String[]::new);
    }

    public static final class SCDastScanWaitForStateIterable extends ArrayList<String> {
        private static final long serialVersionUID = 1L;
        public SCDastScanWaitForStateIterable() {
            super(List.of(getKnownStateNames()));
        }
    }
}
