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
package com.fortify.cli.sc_dast.scan.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.sc_dast._common.session.cli.mixin.SCDastUnirestInstanceSupplierMixin;
import com.fortify.cli.sc_dast.scan.cli.mixin.SCDastScanResolverMixin;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus;
import com.fortify.cli.sc_dast.scan.helper.SCDastScanStatus.SCDastScanStatusIterable;

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
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(SCDastScanStatus::addScanStatus)
                .currentStateProperty("scanStatus")
                .knownStates(SCDastScanStatus.getKnownStateNames())
                .failureStates(SCDastScanStatus.getFailureStateNames())
                .matchStates(states);
    }
}
