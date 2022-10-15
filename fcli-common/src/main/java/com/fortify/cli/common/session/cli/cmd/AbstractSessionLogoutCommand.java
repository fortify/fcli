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
package com.fortify.cli.common.session.cli.cmd;

import com.fortify.cli.common.output.cli.mixin.writer.StandardOutputWriterFactoryMixin;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.ISessionDataManager;
import com.fortify.cli.common.util.FixInjection;
import com.fortify.cli.common.variable.FcliVariableHelper;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess @FixInjection
public abstract class AbstractSessionLogoutCommand<D extends ISessionData> extends AbstractSessionCommand {
    @Getter @Mixin private SessionNameMixin.OptionalParameter sessionNameMixin;
    @Getter @Mixin private StandardOutputWriterFactoryMixin outputWriterFactory;

    @Override
    public final void _run() {
        String sessionName = sessionNameMixin.getSessionName();
        ISessionDataManager<D> sessionDataManager = getSessionDataManager();
        if ( sessionDataManager.exists(sessionName) ) {
            logout(sessionName, sessionDataManager.get(sessionName, true));
            FcliVariableHelper.deleteAllWithPrefix(sessionDataManager.getMinusVariableNamePrefix(sessionName));
            getSessionDataManager().destroy(sessionName);
        }
    }
    
    protected abstract void logout(String sessionName, D sessionData);
    protected abstract ISessionDataManager<D> getSessionDataManager();
}
