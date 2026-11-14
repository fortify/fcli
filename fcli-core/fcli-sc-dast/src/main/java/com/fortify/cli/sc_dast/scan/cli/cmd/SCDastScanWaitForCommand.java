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

import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_dast.scan.cli.mixin.SCDastScanResolverMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus.SCDastScanStatusIterable;
import com.fortify.cli.ssc._common.rest.sc_dast.cli.mixin.SCDastUnirestInstanceSupplierMixin;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SCDastScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin SCDastUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private SCDastScanResolverMixin.PositionalParameterMulti scansResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="Complete", completionCandidates = SCDastScanStatusIterable.class)
    private Set<String> states;
    @Option(names={"--published"})
    private boolean waitForPublish;
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(this::addScanStatus)
                .currentStateProperty("scanStatus")
                .knownStates(Stream.concat(Stream.of(SCDastScanStatus.getKnownStateNames()), Stream.of(STATE_PUBLISHING)).toArray(String[]::new))
                .failureStates(SCDastScanStatus.getFailureStateNames())
                .matchStates(states);
    }
    
    // Completed scans report 'Publishing' until results have been published to SSC
    private JsonNode addScanStatus(JsonNode scanRecord) {
        var record = SCDastScanStatus.addScanStatus(scanRecord);
        if ( waitForPublish && record instanceof ObjectNode objectNode
                && SCDastScanStatus.Complete.name().equals(objectNode.path("scanStatus").asText())
                && !"Published".equals(objectNode.path("publishStatusTypeDescription").asText()) ) {
            objectNode.put("scanStatus", STATE_PUBLISHING);
        }
        return record;
    }
    
    private static final String STATE_PUBLISHING = "Publishing";
}
