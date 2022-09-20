package com.fortify.cli.ssc.session.cli.mixin;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.ssc.session.manager.ISSCUserCredentialsConfig;
import com.fortify.cli.ssc.token.helper.SSCTokenConverter;

import io.micronaut.core.util.StringUtils;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Help.Visibility;
import picocli.CommandLine.Option;

public class SSCSessionLoginOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.MINUTES, Period.DAYS);
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 1, headingKey = "fcli.ssc.session.login.connection.argGroup.heading")
    @Getter private UrlConfigOptions urlConfigOptions;
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2, headingKey = "fcli.ssc.session.login.authentication.argGroup.heading")
    @Getter private SSCAuthOptions authOptions;
    
    public static class SSCAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private SSCCredentialOptions credentialOptions;
    }
    
    public static class SSCCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private SSCUserCredentialOptions userOptions = new SSCUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private SSCTokenCredentialOptions tokenOptions = new SSCTokenCredentialOptions();
    }
    
    public static class SSCUserCredentialOptions extends UserCredentialOptions implements ISSCUserCredentialsConfig {
        @Option(names = {"--expire-in"}, descriptionKey = "fcli.ssc.session.login.expire-in", required = false, defaultValue = "1d", showDefaultValue = Visibility.ALWAYS)
        @Getter private String expireIn;
        
        @Override
        public OffsetDateTime getExpiresAt() {
            return PERIOD_HELPER.getCurrentOffsetDateTimePlusPeriod(expireIn);
        }
    }
    
    public static class SSCTokenCredentialOptions {
        @Option(names = {"--token", "-t"}, descriptionKey = "fcli.ssc.session.login.token", required = true, interactive = true, arity = "0..1", echo = false)
        @Getter private char[] token;
    }
    
    public SSCUserCredentialOptions getUserCredentialOptions() {
        return Optional.ofNullable(authOptions).map(SSCAuthOptions::getCredentialOptions).map(SSCCredentialOptions::getUserOptions).orElse(null);
    }
    
    public SSCTokenCredentialOptions getTokenCredentialOptions() {
        return Optional.ofNullable(authOptions).map(SSCAuthOptions::getCredentialOptions).map(SSCCredentialOptions::getTokenOptions).orElse(null);
    }
    
    public char[] getRestToken() {
        SSCTokenCredentialOptions tokenCredentialOptions = getTokenCredentialOptions();
        return tokenCredentialOptions==null || tokenCredentialOptions.getToken()==null
                ? null : SSCTokenConverter.toRestToken(tokenCredentialOptions.getToken());
    }

    public final boolean hasUserCredentials() {
        SSCUserCredentialOptions userCredentialOptions = getUserCredentialOptions();
        return userCredentialOptions!=null 
                && StringUtils.isNotEmpty(userCredentialOptions.getUser())
                && userCredentialOptions.getPassword()!=null;
    }
}
