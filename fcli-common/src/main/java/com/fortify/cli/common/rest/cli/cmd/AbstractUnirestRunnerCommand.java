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

import com.fortify.cli.common.cli.cmd.AbstractFortifyCLICommand;
import com.fortify.cli.common.rest.runner.IUnirestWithSessionDataRunner;
import com.fortify.cli.common.session.manager.api.ISessionData;

import kong.unirest.UnirestInstance;
import lombok.SneakyThrows;

public abstract class AbstractUnirestRunnerCommand<D extends ISessionData> extends AbstractFortifyCLICommand implements Runnable {
    @Override @SneakyThrows
    public final void run() {
        // TODO Do we want to do anything with the results, like formatting it based on output options?
        //      Or do we let the actual implementation handle this?
        getUnirestRunner().run(this::run);
    }
    
    protected Void run(UnirestInstance unirest, D sessionData) {
        return run(unirest);
    }
    
    // TODO Eventually, we'll likely want to change all command implementations to implement
    //      the run(UnirestInstance, SessionData) method; we can then remove this method and
    //      make the run(UnirestInstance, Session) method abstract.
    protected Void run(UnirestInstance unirest) {
        throw new RuntimeException("Command must implement either run(UnirestInstance,SessionData) or run(UnirestInstance)");
    }
    
    protected abstract IUnirestWithSessionDataRunner<D> getUnirestRunner();
}
