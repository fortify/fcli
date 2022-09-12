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
package com.fortify.cli.ssc.session.cli.cmd;

import com.fortify.cli.common.session.cli.cmd.AbstractSessionLogoutCommand;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.session.manager.api.IUserCredentials;
import com.fortify.cli.common.session.manager.spi.ISessionLogoutHandler;
import com.fortify.cli.ssc.session.manager.SSCSessionLogoutHandler;
import com.fortify.cli.ssc.util.SSCConstants;

import jakarta.inject.Inject;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "logout", sortOptions = false, resourceBundle = "com.fortify.cli.ssc.i18n.SSCMessages")
public class SSCSessionLogoutCommand extends AbstractSessionLogoutCommand<IUserCredentials> {
    @Inject SSCSessionLogoutHandler logoutHandler;
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1, headingKey = "fcli.ssc.session.logout.authentication.argGroup.heading")
    @Getter private SSCAuthOptions authOptions;
    
    static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
        @Getter private SSCCredentialOptions credentialOptions;
    }
    
    static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private UserCredentialOptions userOptions;
        @Option(names={"--no-revoke-token"})
        @Getter private boolean noRevokeToken; // If this option is specified, then userOptions will be null and token will not be revoked
    }
    
    @Override
    public String getSessionType() {
        return SSCConstants.SESSION_TYPE;
    }

    @Override
    protected IUserCredentials getLogoutConfig() {
        return getAuthOptions().getCredentialOptions().getUserOptions();
    }

    @Override
    protected ISessionLogoutHandler<IUserCredentials> getLogoutHandler() {
        return logoutHandler;
    }
}
