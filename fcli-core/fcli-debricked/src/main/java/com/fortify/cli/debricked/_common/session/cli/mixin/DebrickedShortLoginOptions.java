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
package com.fortify.cli.debricked._common.session.cli.mixin;

import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.debricked._common.session.helper.IDebrickedLoginOptions;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class DebrickedShortLoginOptions implements IDebrickedLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private DebrickedShortUrlConfigOptions urlConfigOptions = new DebrickedShortUrlConfigOptions();
    
    @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private DebrickedShortAuthOptions authOptions = new DebrickedShortAuthOptions();
    
    public static class DebrickedShortAuthOptions implements IDebrickedLoginOptions.IDebrickedAuthOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private DebrickedShortUserCredentialOptions userCredentialsOptions;
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private DebrickedShortAccessTokenCredentialOptions tokenOptions;
    }
    
    public static class DebrickedShortUserCredentialOptions implements IDebrickedLoginOptions.IDebrickedUserCredentialOptions {
        @Option(names = {"--user", "-u"}, required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.medium, description = "DEBRICKED USER")
        @Getter private String user;
        
        @Option(names = {"--password", "-p"}, interactive = true, echo = false, arity = "0..1", required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "DEBRICKED PASSWORD")
        @Getter private char[] password;
    }
    
    public static class DebrickedShortAccessTokenCredentialOptions implements IDebrickedLoginOptions.IDebrickedAccessTokenCredentialOptions {
        @Option(names = {"--access-token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "DEBRICKED TOKEN")
        @Getter private char[] accessToken;
    }
}