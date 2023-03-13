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

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.spi.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.manager.api.ISessionData;
import com.fortify.cli.common.session.manager.spi.ISessionDataManager;

import io.micronaut.core.annotation.ReflectiveAccess;
import lombok.Getter;
import picocli.CommandLine.Mixin;

@ReflectiveAccess
public abstract class AbstractSessionLogoutCommand<D extends ISessionData> extends AbstractSessionCommand implements IActionCommandResultSupplier {
    @Getter @Mixin private SessionNameMixin.OptionalParameter sessionNameMixin;
    
    @Override
    protected JsonNode getJsonNode() {
        String sessionName = sessionNameMixin.getSessionName();
        JsonNode result = null;
        ISessionDataManager<D> sessionDataManager = getSessionDataManager();
        if ( sessionDataManager.exists(sessionName) ) {
        	result = sessionDataManager.sessionSummaryAsObjectNode(sessionName);
            logout(sessionName, sessionDataManager.get(sessionName, true));
            // TODO Optionally delete all variables
            getSessionDataManager().destroy(sessionName);
        }
        return result;
    }
    
    @Override
    public String getActionCommandResult() {
    	return "TERMINATED";
    }
    
    @Override
    public boolean isSingular() {
    	return false;
    }
    
    protected abstract void logout(String sessionName, D sessionData);
    protected abstract ISessionDataManager<D> getSessionDataManager();
}
