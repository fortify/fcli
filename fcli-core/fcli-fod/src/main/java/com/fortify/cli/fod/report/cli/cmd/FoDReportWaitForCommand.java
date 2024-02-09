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
package com.fortify.cli.fod.report.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.scan.helper.FoDScanStatus;
import com.fortify.cli.fod._common.session.cli.mixin.FoDUnirestInstanceSupplierMixin;
import com.fortify.cli.fod.report.cli.mixin.FoDReportResolverMixin;
import com.fortify.cli.fod.report.helper.FoDReportStatus;
import com.fortify.cli.fod.report.helper.FoDReportStatus.FoDReportStatusIterable;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

import java.util.Set;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class FoDReportWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin private FoDUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private FoDReportResolverMixin.PositionalParameterMulti reportResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="Completed", completionCandidates = FoDReportStatusIterable.class)
    private Set<String> states;

    @Override
    protected final WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(reportResolver::getReportDescriptorJsonNodes)
                .recordTransformer(FoDReportStatus::addReportStatus)
                .currentStateProperty("reportStatusType")
                .knownStates(FoDScanStatus.getKnownStateNames())
                .failureStates(FoDScanStatus.getFailureStateNames())
                .matchStates(states);
    }

}
