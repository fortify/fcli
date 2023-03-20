/*******************************************************************************
 * (c) Copyright 2020 Micro Focus or one of its affiliates
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

package com.fortify.cli.fod.scan.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.rest.cli.cmd.AbstractWaitForCommand;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.fod.rest.cli.mixin.FoDUnirestRunnerMixin;
import com.fortify.cli.fod.scan.cli.mixin.FoDScanResolverMixin;
import com.fortify.cli.fod.scan.helper.FoDScanHelper;
import com.fortify.cli.fod.scan.helper.FoDScanStatus;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command(name = BasicOutputHelperMixins.WaitFor.CMD_NAME)
public class FoDScanWaitForCommand extends AbstractWaitForCommand {
    @Getter @Mixin FoDUnirestRunnerMixin unirestRunner;
    @Mixin private FoDScanResolverMixin.PositionalParameterMulti scansResolver;

    @Override
    protected WaitHelperBuilder configure(WaitHelperBuilder builder) {
        return builder
                .recordsSupplier(scansResolver::getScanDescriptorJsonNodes)
                .recordTransformer(FoDScanHelper::renameFields)
                .currentStateProperty("analysisStatusType")
                .knownStates(FoDScanStatus.getKnownStateNames())
                .failureStates(FoDScanStatus.getFailureStateNames())
                .defaultCompleteStates(FoDScanStatus.getDefaultCompleteStateNames());
    }

}
