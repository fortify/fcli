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
package com.fortify.cli.common.output.cli.cmd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.cli.cmd.AbstractRunnableCommand;
import com.fortify.cli.common.json.JsonHelper;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.writer.ISingularSupplier;

public abstract class AbstractCheckCommand extends AbstractRunnableCommand implements ISingularSupplier {
    @Override
    public final Integer call() {
        initMixins();
        IOutputHelper outputHelper = getOutputHelper();
        if ( isPass() ) {
            outputHelper.write(getPassResult());
            return 0;
        } else {
            outputHelper.write(getFailResult());
            return 1;
        }
    }
    
    @Override
    public final boolean isSingular() {
        return true;
    }
    
    protected abstract IOutputHelper getOutputHelper();
    protected abstract boolean isPass();
    
    protected String getPropertyName() { return "result"; }
    protected String getPassValue() { return "Pass"; }
    protected String getFailValue() { return "Fail"; }
    protected ObjectNode getPassResult() {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put(getPropertyName(), getPassValue());
    }
    protected ObjectNode getFailResult() {
        return JsonHelper.getObjectMapper().createObjectNode()
                .put(getPropertyName(), getFailValue());
    }
}
