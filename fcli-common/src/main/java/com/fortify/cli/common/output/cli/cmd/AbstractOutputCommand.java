/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.common.output.cli.cmd;

import java.util.Arrays;
import java.util.List;

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.output.cli.mixin.IOutputHelper;
import com.fortify.cli.common.output.writer.ISingularSupplier;

public abstract class AbstractOutputCommand extends AbstractFortifyCLICommand implements Runnable, ISingularSupplier {
    private static final List<Class<?>> supportedInterfaces = Arrays.asList(
            IBaseRequestSupplier.class, 
            IJsonNodeSupplier.class);
    @Override
    public final void run() {
        initMixins();
        IOutputHelper outputHelper = getOutputHelper();
        if ( isInstance(IBaseRequestSupplier.class) ) {
            outputHelper.write(((IBaseRequestSupplier)this).getBaseRequest());
        } else if ( isInstance(IJsonNodeSupplier.class) ) {
            outputHelper.write(((IJsonNodeSupplier)this).getJsonNode());
        } else {
            throw new IllegalStateException(this.getClass().getName()+" must implement exactly one of "+supportedInterfaces);
        }
    }
    
    private boolean isInstance(Class<?> clazz) {
        return clazz.isAssignableFrom(this.getClass()) &&
                supportedInterfaces.stream()
                .filter(c->!c.equals(clazz))
                .noneMatch(c->c.isAssignableFrom(this.getClass()));
    }
    
    protected abstract IOutputHelper getOutputHelper();
}
