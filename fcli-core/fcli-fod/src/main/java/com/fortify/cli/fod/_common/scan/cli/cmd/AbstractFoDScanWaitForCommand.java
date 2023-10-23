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

package com.fortify.cli.fod._common.scan.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.fod._common.output.mixin.FoDProductHelperStandardMixin;
import com.fortify.cli.fod._common.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus.FoDScanStatusIterable;
import com.fortify.cli.fod._common.scan.helper.FoDScanType;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@CommandGroup("*-scan")
public abstract class AbstractFoDScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin FoDProductHelperStandardMixin productHelper;
    @Mixin private FoDScanResolverMixin.PositionalParameterMulti scansResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="Completed", completionCandidates = FoDScanStatusIterable.class)
    private Set<String> states;

    @Override
    protected final WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .currentStateProperty("analysisStatusType")
                .knownStates(FoDScanStatus.getKnownStateNames())
                .failureStates(FoDScanStatus.getFailureStateNames())
                .matchStates(states);
    }
    
    // TODO Verify that all given scan id's are of given scan type
    protected abstract FoDScanType getScanType();

}
