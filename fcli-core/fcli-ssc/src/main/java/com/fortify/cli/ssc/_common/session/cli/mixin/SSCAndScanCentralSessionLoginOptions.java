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

import com.fortify.cli.common.rest.cli.mixin.ConnectionConfigOptions;
import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralCredentialsConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCAndScanCentralUrlConfig;
import com.fortify.cli.ssc._common.session.helper.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.access_control.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

public class SSCAndScanCentralSessionLoginOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAndScanCentralUrlConfigOptions sscAndScanCentralUrlConfigOptions = new SSCAndScanCentralUrlConfigOptions();
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private SSCAndScanCentralCredentialConfigOptions sscAndScanCentralCredentialConfigOptions = new SSCAndScanCentralCredentialConfigOptions();
    
    public static class SSCAndScanCentralUrlConfigOptions extends ConnectionConfigOptions implements ISSCAndScanCentralUrlConfig {
        @Option(names = {"--url"}, required = true, order=1)
        @Getter private String sscUrl;
        
        @Option(names = {"--sc-sast-url"}, required = false, order=1)
        @Getter private String scSastControllerUrl;
    }
    
    public static class SSCAndScanCentralCredentialConfigOptions implements ISSCAndScanCentralCredentialsConfig {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 1) 
        private  SSCCredentialOptions sscCredentialOptions = new SSCCredentialOptions();
        @Option(names = {"--client-auth-token", "-c"}, required = false, interactive = true, arity = "0..1", echo = false) 
        @Getter private char[] scSastClientAuthToken;
        
        public ISSCUserCredentialsConfig getSscUserCredentialsConfig() {
            return sscCredentialOptions==null ? null : sscCredentialOptions.getUserCredentialsConfig();
        }
        
        @Override
        public char[] getSscToken() {
            var tokenOptions = sscCredentialOptions==null ? null : sscCredentialOptions.tokenOptions;
            return tokenOptions!=null && tokenOptions.token!=null 
                ? SSCTokenConverter.toRestToken(tokenOptions.token)
                : null;
        }
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SSCUserCredentialOptions userCredentialsConfig = new SSCUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SSCTokenCredentialOptions tokenOptions = new SSCTokenCredentialOptions();
    }
    
    public static class SSCUserCredentialOptions extends UserCredentialOptions implements ISSCUserCredentialsConfig {
        @Option(names = {"--expire-in"}, required = false, defaultValue = "3d")
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
    
    @Command
    public static final class SSCUrlConfigOptions extends UrlConfigOptions {
        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }
}
