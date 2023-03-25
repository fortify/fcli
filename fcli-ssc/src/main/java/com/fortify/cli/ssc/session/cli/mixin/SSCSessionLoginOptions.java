package com.fortify.cli.ssc.session.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.entity.token.helper.SSCTokenConverter;
import com.fortify.cli.ssc.session.helper.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.helper.ISSCUserCredentialsConfig;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SSCSessionLoginOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private UrlConfigOptions urlConfigOptions = new UrlConfigOptions();
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private SSCAuthOptions authOptions = new SSCAuthOptions();
    
    public ISSCCredentialsConfig getCredentialsConfig() {
        return authOptions==null ? null : authOptions.getCredentialOptions();
    }
    
    public ISSCUserCredentialsConfig getUserCredentialsConfig() {
        return getCredentialsConfig()==null ? null : getCredentialsConfig().getUserCredentialsConfig();
    }
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions = new SSCCredentialOptions();
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
}
