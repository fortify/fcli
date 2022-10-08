package com.fortify.cli.ssc.session.cli.mixin;

import java.time.OffsetDateTime;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.session.manager.ISSCCredentialsConfig;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SSCSessionLoginOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
    @Getter private UrlConfigOptions urlConfigOptions;
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private SSCAuthOptions authOptions;
    
    public ISSCCredentialsConfig getCredentialsConfig() {
        return authOptions==null ? null : authOptions.getCredentialOptions();
    }
    
    public ISSCUserCredentialsConfig getUserCredentialsConfig() {
        return getCredentialsConfig()==null ? null : getCredentialsConfig().getUserCredentialsConfig();
    }
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions;
    }
    
    public static class SSCCredentialOptions implements ISSCCredentialsConfig {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SSCUserCredentialOptions userCredentialsConfig = new SSCUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SSCTokenCredentialOptions tokenOptions = new SSCTokenCredentialOptions();
        
        @Override
        public char[] getPredefinedToken() {
            return tokenOptions==null || tokenOptions.token==null ? null : SSCTokenConverter.toRestToken(tokenOptions.token);
        }
    }
    
    public static class SSCUserCredentialOptions extends UserCredentialOptions implements ISSCUserCredentialsConfig {
        @Option(names = {"--expire-in"}, descriptionKey = "fcli.ssc.session.expire-in", required = false, defaultValue = "1d", showDefaultValue = Visibility.ALWAYS)
        @Getter private String expireIn;
        
        @Override
        public OffsetDateTime getExpiresAt() {
            return PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
        }
    }
    
    public static class SSCTokenCredentialOptions {
        @Option(names = {"--token", "-t"}, descriptionKey = "fcli.ssc.session.token", required = true, interactive = true, echo = false)
        @Getter private char[] token;
    }
}
