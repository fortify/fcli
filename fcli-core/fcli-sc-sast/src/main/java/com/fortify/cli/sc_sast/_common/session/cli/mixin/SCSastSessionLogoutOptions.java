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
package com.fortify.cli.sc_sast._common.session.cli.mixin;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class SCSastSessionLogoutOptions {
    @ArgGroup(exclusive = false, multiplicity = "0..1") 
    @Getter private SCSastUserCredentialOptions userCredentialOptions;
    @Option(names={"--no-revoke-token"})
    @Getter private boolean noRevokeToken = false;
}
