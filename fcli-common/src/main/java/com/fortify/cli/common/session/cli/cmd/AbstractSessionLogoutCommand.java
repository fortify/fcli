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
package com.fortify.cli.common.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSessionLogoutCommand<D extends ISessionDescriptor> extends AbstractSessionCommand<D> implements IActionCommandResultSupplier {
    @Getter @Mixin private SessionNameMixin.OptionalParameter sessionNameMixin;
    
    @Override
    public JsonNode getJsonNode() {
        String sessionName = sessionNameMixin.getSessionName();
        JsonNode result = null;
        var sessionHelper = getSessionHelper();
        if ( sessionHelper.exists(sessionName) ) {
        	result = sessionHelper.sessionSummaryAsObjectNode(sessionName);
            logout(sessionName, sessionHelper.get(sessionName, true));
            // TODO Optionally delete all variables
            getSessionHelper().destroy(sessionName);
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
    
    protected abstract void logout(String sessionName, D sessionDescriptor);
}
