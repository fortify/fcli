/*******************************************************************************
 * Copyright 2021, 2022 Open Text.
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
package com.fortify.cli.sc_dast._common.session.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCDastSessionLogoutOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAuthOptions authOptions = new SSCAuthOptions();
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SCDastUserCredentialOptions userOptions= new SCDastUserCredentialOptions();
        @Option(names={"--no-revoke-token"})
        @Getter private boolean noRevokeToken;
    }
    
    public SCDastUserCredentialOptions getUserCredentialOptions() {
        return authOptions.credentialOptions.noRevokeToken ? null : authOptions.credentialOptions.userOptions;
    }
}
