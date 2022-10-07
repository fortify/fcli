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

import com.fortify.cli.common.output.cli.mixin.spi.output.IUnirestOutputHelper;
import com.fortify.cli.common.rest.cli.cmd.AbstractUnirestRunnerCommand;

import io.micronaut.core.annotation.ReflectiveAccess;
import kong.unirest.UnirestInstance;

@ReflectiveAccess
public abstract class AbstractUnirestOutputCommand extends AbstractUnirestRunnerCommand {
    @Override
    protected final Void run(UnirestInstance unirest) {
        IUnirestOutputHelper outputHelper = getOutputHelper();
        if ( isBaseHttpRequestSupplier() ) {
            outputHelper.write(unirest, ((IBaseHttpRequestSupplier)this).getBaseRequest(unirest));
        } else if ( isJsonNodeSupplier() ) {
            outputHelper.write(unirest, ((IJsonNodeSupplier)this).getJsonNode(unirest));
        } else {
            throw new IllegalStateException(this.getClass().getName()+" must implement exactly one of I[BaseHttpRequest|JsonNodeHolder|JsonNode]Supplier");
        }
        return null;
    }
    
    private boolean isBaseHttpRequestSupplier() {
        return (this instanceof IBaseHttpRequestSupplier)
                && !(this instanceof IJsonNodeSupplier);
    }
    
    private boolean isJsonNodeSupplier() {
        return !(this instanceof IBaseHttpRequestSupplier)
                && (this instanceof IJsonNodeSupplier);
    }
    protected abstract IUnirestOutputHelper getOutputHelper();
}
