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
