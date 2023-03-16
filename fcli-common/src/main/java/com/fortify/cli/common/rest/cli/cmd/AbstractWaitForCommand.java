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
package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.BasicOutputHelperMixins;
import com.fortify.cli.common.output.spi.ISingularSupplier;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.rest.cli.mixin.StandardWaitHelperProgressMonitorMixin;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperControlOptions;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperWaitOptions;
import com.fortify.cli.common.rest.wait.WaitHelper;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;
import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractWaitForCommand<D extends ISessionData> extends AbstractUnirestRunnerCommand<D> implements IActionCommandResultSupplier, ISingularSupplier {
    @Getter @Mixin private BasicOutputHelperMixins.WaitFor outputHelper;
    @Mixin private WaitHelperControlOptions controlOptions;
    @Mixin private WaitHelperWaitOptions waitOptions;
    @Mixin StandardWaitHelperProgressMonitorMixin progressMonitorMixin;
    
    @Override
    protected Void run(UnirestInstance unirest) {
        configure(
            WaitHelper.builder()
                .controlProperties(controlOptions)
                .progressMonitor(progressMonitorMixin.createProgressMonitor(false))
                .onFinish(WaitHelper::recordsWithActionAsArrayNode, outputHelper::write)
        ).build().wait(unirest, waitOptions);
        return null;
    }
    
    protected abstract WaitHelperBuilder configure(WaitHelperBuilder builder);

    @Override
    public String getActionCommandResult() {
        return "N/A"; // Action result will be provided by WaitHelper
    }
    
    @Override
    public boolean isSingular() {
        return false;
    }
}
