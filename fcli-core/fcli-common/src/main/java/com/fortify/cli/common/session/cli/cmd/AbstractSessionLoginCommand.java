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
package com.fortify.cli.common.session.cli.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.output.transform.IActionCommandResultSupplier;
import com.fortify.cli.common.session.cli.mixin.SessionNameMixin;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

import lombok.Getter;
import picocli.CommandLine.Mixin;

public abstract class AbstractSessionLoginCommand<D extends ISessionDescriptor> extends AbstractSessionCommand<D> implements IActionCommandResultSupplier {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionLoginCommand.class);
    @Getter @Mixin private SessionNameMixin.OptionalLoginOption sessionNameMixin;
    
    @Override
    public JsonNode getJsonNode() {
    	String sessionName = sessionNameMixin.getSessionName();
        var sessionHelper = getSessionHelper();
        logoutIfSessionExists(sessionName);
        D sessionDescriptor = login(sessionName);
        sessionHelper.save(sessionName, sessionDescriptor);
        testAuthenticatedConnection(sessionName);
        return sessionHelper.sessionSummaryAsObjectNode(sessionName);
    }
    
    @Override
    public String getActionCommandResult() {
    	return "CREATED";
    }
    
    @Override
    public boolean isSingular() {
    	return true;
    }

    private void logoutIfSessionExists(String sessionName) {
        var sessionHelper = getSessionHelper();
        if ( sessionHelper.exists(sessionName) ) {
            try {
                logoutBeforeNewLogin(sessionName, sessionHelper.get(sessionName, false));
            } catch ( Exception e ) {
                LOG.warn("Error logging out previous session");
                LOG.debug("Exception details:", e);
            } finally {
                sessionHelper.destroy(sessionName);
            }
        }
    }

    protected abstract void logoutBeforeNewLogin(String sessionName, D sessionDescriptor);
    protected abstract D login(String sessionName);
    protected void testAuthenticatedConnection(String sessionName) {}
}
