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
package com.fortify.cli.ssc._common.session.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc._common.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SSCSessionLoginOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private UrlConfigOptions urlConfigOptions = new UrlConfigOptions();
    
    @ArgGroup(exclusive = true, multiplicity = "1", order = 2)
    @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
    
    public ISSCUserCredentialsConfig getUserCredentialsConfig() {
        return getCredentialOptions()==null ? null : getCredentialOptions().getUserCredentialsConfig();
    }
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SSCUserCredentialOptions userCredentialsConfig = new SSCUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SSCTokenCredentialOptions tokenOptions = new SSCTokenCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SSCCITokenCredentialOptions ciTokenOptions = new SSCCITokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            if ( tokenOptions!=null && tokenOptions.token!=null ) {
                return SSCTokenConverter.toRestToken(tokenOptions.token);
            } else if ( ciTokenOptions!=null && ciTokenOptions.token!=null ) {
                return SSCTokenConverter.toRestToken(ciTokenOptions.token);
            } else {
                return null;
            }
        }
    }
    
    public static class SSCUserCredentialOptions extends UserCredentialOptions implements ISSCUserCredentialsConfig {
        @Option(names = {"--expire-in"}, required = false, defaultValue = "1d", showDefaultValue = Visibility.ALWAYS)
        @Getter private String expireIn;
        
        @Override
        public OffsetDateTime getExpiresAt() {
            return PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
        }
    }
    
    public static class SSCTokenCredentialOptions {
        @Option(names = {"--token", "-t"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] token;
    }
    
    public static class SSCCITokenCredentialOptions {
        @Option(names = {"--ci-token"}, interactive = true, echo = false, arity = "0..1", required = true)
        @Getter private char[] token;
    }
    
    @Command
    public static final class SSCUrlConfigOptions extends UrlConfigOptions {
        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }
}
