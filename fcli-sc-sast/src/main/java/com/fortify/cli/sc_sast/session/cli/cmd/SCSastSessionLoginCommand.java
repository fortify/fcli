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
package com.fortify.cli.sc_sast.session.cli.cmd;

import com.fasterxml.jackson.databind.JsonNode;
import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.rest.runner.config.UnirestUnexpectedHttpResponseConfigurer;
import com.fortify.cli.common.session.cli.cmd.AbstractSessionLoginCommand;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionData;
import com.fortify.cli.sc_sast.session.manager.SCSastSessionDataManager;

import jakarta.inject.Inject;
import kong.unirest.UnirestInstance;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "login", sortOptions = false)
public class SCSastSessionLoginCommand extends AbstractSessionLoginCommand<SCSastSessionData> {
    @Getter @Inject private SCSastSessionDataManager sessionDataManager;
    
    @ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.sc-sast-connection-options.heading", order = 1)
    @Getter private UrlConfigOptions urlConfigOptions;
    
    @ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.sc-sast-authentication-options.heading", order = 2)
    @Getter private SCSastAuthOptions authOptions;
    
    static class SCSastAuthOptions {
        @Option(names = {"--client-auth-token", "-t"}, required = true, interactive = true, arity = "0..1", echo = false) 
        @Getter private char[] clientAuthToken;
    }
    
    @Override
    protected void logoutBeforeNewLogin(String sessionName, SCSastSessionData sessionData) {
        // TODO Nothing to do for now
    }
    
    @Override
    protected SCSastSessionData login(String sessionName) {
        return new SCSastSessionData(urlConfigOptions, authOptions.getClientAuthToken());
    }
    
    @Override
    protected void testAuthenticatedConnection(String sessionName) {
        super.testAuthenticatedConnection(sessionName);
        // TODO Call testWithUnirest()
    }
    
    protected Void testWithUnirest(UnirestInstance unirest) {
        // TODO Review this
        UnirestUnexpectedHttpResponseConfigurer.configure(unirest);
        unirest.get("/rest/v2/ping").asObject(JsonNode.class).getBody();
        return null;
    }
    
    
}
