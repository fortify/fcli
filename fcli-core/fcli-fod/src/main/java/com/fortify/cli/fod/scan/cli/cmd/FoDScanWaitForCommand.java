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

package com.fortify.cli.fod.scan.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.fod._common.output.mixin.FoDProductHelperStandardMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanStatus;
import com.fortify.cli.fod.scan.helper.FoDScanStatus.FoDScanStatusIterable;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME, hidden = false)
public class FoDScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin FoDProductHelperStandardMixin productHelper;
    @Mixin private FoDScanResolverMixin.PositionalParameterMulti scansResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="Completed", completionCandidates = FoDScanStatusIterable.class)
    private Set<String> states;

    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(FoDScanHelper::renameFields)
                .currentStateProperty("analysisStatusType")
                .knownStates(FoDScanStatus.getKnownStateNames())
                .failureStates(FoDScanStatus.getFailureStateNames())
                .matchStates(states);
    }

}
