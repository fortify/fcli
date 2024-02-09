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
package com.fortify.cli.ssc.report.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.ssc._common.session.cli.mixin.SSCUnirestInstanceSupplierMixin;
import com.fortify.cli.ssc.report.cli.mixin.SSCReportResolverMixin;
import com.fortify.cli.ssc.report.helper.SSCReportStatus;
import com.fortify.cli.ssc.report.helper.SSCReportStatus.SSCReportStatusIterable;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SSCReportWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin private SSCUnirestInstanceSupplierMixin unirestInstanceSupplier;
    @Mixin private SSCReportResolverMixin.PositionalParameterMulti reportsResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="PROCESS_COMPLETE", completionCandidates = SSCReportStatusIterable.class)
    private Set<String> states;
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(reportsResolver::getReportDescriptorJsonNodes)
                .currentStateProperty("status")
                .knownStates(SSCReportStatus.getKnownStateNames())
                .failureStates(SSCReportStatus.getFailureStateNames())
                .matchStates(states);
    }
}
