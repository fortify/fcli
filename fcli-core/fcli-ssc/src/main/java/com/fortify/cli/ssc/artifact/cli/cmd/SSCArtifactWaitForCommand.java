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
package com.fortify.cli.ssc.artifact.cli.cmd;

import java.util.Set;

import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.ssc._common.output.cli.mixin.SSCProductHelperStandardMixin;
import com.fortify.cli.ssc.artifact.cli.mixin.SSCArtifactResolverMixin;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactHelper;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus;
import com.fortify.cli.ssc.artifact.helper.SSCArtifactStatus.SSCArtifactStatusIterable;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = OutputHelperMixins.WaitFor.CMD_NAME)
public class SSCArtifactWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin SSCProductHelperStandardMixin productHelper;
    @Mixin private SSCArtifactResolverMixin.PositionalParameterMulti artifactsResolver;
    @Option(names={"-s", "--any-state"}, required=true, split=",", defaultValue="PROCESS_COMPLETE", completionCandidates = SSCArtifactStatusIterable.class)
    private Set<String> states;
    
    @Override
    protected WaitHelperBuilder configure(UnirestInstance unirest, WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(artifactsResolver::getArtifactDescriptorJsonNodes)
                .recordTransformer(SSCArtifactHelper::addScanTypes)
                .currentStateProperty("status")
                .knownStates(SSCArtifactStatus.getKnownStateNames())
                .failureStates(SSCArtifactStatus.getFailureStateNames())
                .matchStates(states);
    }
}
