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
package com.fortify.cli.ssc.system_state.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.cli.util.CommandGroup;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCProductHelperStandardMixin;
import com.fortify.cli.ssc.system_state.cli.mixin.SSCJobResolverMixin;
import com.fortify.cli.ssc.system_state.helper.SSCJobStatus;
import com.fortify.cli.ssc.system_state.helper.SSCJobStatus.SSCJobStatusIterable;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "wait-for-job") @CommandGroup("job")
public class SSCStateJobWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin SSCProductHelperStandardMixin productHelper;
    @Mixin private SSCJobResolverMixin.PositionalParameterMulti jobsResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="FINISHED", completionCandidates = SSCJobStatusIterable.class)
    private Set<String> states;
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(jobsResolver::getJobDescriptorJsonNodes)
                .currentStateProperty("state")
                .knownStates(SSCJobStatus.getKnownStateNames())
                .failureStates(SSCJobStatus.getFailureStateNames())
                .matchStates(states);
    }
}
