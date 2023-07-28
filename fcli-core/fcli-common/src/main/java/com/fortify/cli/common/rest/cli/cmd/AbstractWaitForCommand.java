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
package com.fortify.cli.common.rest.cli.cmd;

import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.output.cli.mixin.OutputHelperMixins;
import com.fortify.cli.common.output.product.IProductHelperSupplier;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.output.writer.ISingularSupplier;
import com.fortify.cli.common.rest.cli.mixin.StandardWaitHelperProgressMonitorMixin;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperControlOptions;
import com.fortify.cli.common.rest.cli.mixin.WaitHelperWaitOptions;
import com.fortify.cli.common.rest.unirest.IUnirestInstanceSupplier;
import com.fortify.cli.common.rest.wait.WaitHelper;
import com.fortify.cli.common.rest.wait.WaitHelper.WaitHelperBuilder;

import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractWaitForCommand extends AbstractRunnableCommand implements IActionCommandResultSupplier, IProductHelperSupplier, ISingularSupplier, Runnable {
    @Getter @Mixin private OutputHelperMixins.WaitFor outputHelper;
    @Mixin private WaitHelperControlOptions controlOptions;
    @Mixin private WaitHelperWaitOptions waitOptions;
    @Mixin StandardWaitHelperProgressMonitorMixin progressMonitorMixin;
    
    @Override
    public void run() {
        initMixins();
        var productHelper = getProductHelper();
        if ( productHelper instanceof IUnirestInstanceSupplier ) {
            UnirestInstance unirest = ((IUnirestInstanceSupplier)productHelper).getUnirestInstance();
            wait(unirest);
        } else {
            throw new RuntimeException("Class doesn't implement IUnirestInstanceSupplier: "+productHelper.getClass().getName());
        }
    }
    
    private void wait(UnirestInstance unirest) {
        configure(
                WaitHelper.builder()
                    .controlProperties(controlOptions)
                    .progressMonitor(progressMonitorMixin.create(false))
                    .onFinish(WaitHelper::recordsWithActionAsArrayNode, outputHelper::write)
            ).build().wait(unirest, waitOptions);
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
