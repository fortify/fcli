/*******************************************************************************
 * (c) Copyright 2021 Micro Focus or one of its affiliates
 *
 * Permission is hereby granted, free of charge, to any person obtaining a 
 * copy of this software and associated documentation files (the 
 * "Software"), to deal in the Software without restriction, including without 
 * limitation the rights to use, copy, modify, merge, publish, distribute, 
 * sublicense, and/or sell copies of the Software, and to permit persons to 
 * whom the Software is furnished to do so, subject to the following 
 * conditions:
 * 
 * The above copyright notice and this permission notice shall be included 
 * in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, 
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS 
 * IN THE SOFTWARE.
 ******************************************************************************/
package com.fortify.cli.ssc.appversion_artifact.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.ssc.appversion_artifact.cli.mixin.SSCAppVersionArtifactResolverMixin;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactHelper;
import com.fortify.cli.ssc.appversion_artifact.helper.SSCAppVersionArtifactStatus;
import com.fortify.cli.ssc.rest.cli.mixin.SSCUnirestRunnerMixin;
import com.fortify.cli.ssc.session.manager.ISSCSessionData;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.WaitFor.CMD_NAME)
public class SSCAppVersionArtifactWaitForCommand extends AbstractWaitForCommand<ISSCSessionData> {
    @Getter @Mixin SSCUnirestRunnerMixin unirestRunner;
    @Mixin private SSCAppVersionArtifactResolverMixin.PositionalParameterMulti artifactsResolver;
    
    @Override
    protected WaitHelperBuilder configure(WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(artifactsResolver::getArtifactDescriptorJsonNodes)
                .recordTransformer(SSCAppVersionArtifactHelper::addScanTypes)
                .currentStateProperty("status")
                .knownStates(SSCAppVersionArtifactStatus.getKnownStateNames())
                .failureStates(SSCAppVersionArtifactStatus.getFailureStateNames())
                .defaultCompleteStates(SSCAppVersionArtifactStatus.getDefaultCompleteStateNames());
    }
}
