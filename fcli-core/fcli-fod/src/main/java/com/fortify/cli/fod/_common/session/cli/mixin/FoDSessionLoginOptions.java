/*
 * Copyright 2021-2025 Open Text.
 *
 * The only warranties for products and services of Open Text
 * and its affiliates and licensors ("Open Text") are as may
 * be set forth in the express warranty statements accompanying
 * such products and services. Nothing herein should be construed
 * as constituting an additional warranty. Open Text shall not be
 * liable for technical or editorial errors or omissions contained
 * herein. The information contained herein is subject to change
 * without notice.
 */
package com.fortify.cli.fod._common.session.cli.mixin;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDClientCredentials;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDUserCredentials;

import lombok.Getter;
import lombok.SneakyThrows;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class FoDSessionLoginOptions {
    @Mixin @Getter private FoDUrlConfigOptions urlConfigOptions = new FoDUrlConfigOptions();
    
    @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
    @Getter private FoDAuthOptions authOptions = new FoDAuthOptions();
    
    public static class FoDAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private FoDCredentialOptions credentialOptions = new FoDCredentialOptions();
        @Option(names="--scopes", defaultValue="api-tenant", split=",")
        @Getter private String[] scopes;
        @Option(names = {"-t", "--tenant"}, required = false)
        @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TENANT")
        @Getter private String tenant; // Optional: required only for user credentials
    }
    
    public static class FoDCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private UserCredentialOptions userCredentialOptions = new UserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }
    
    public static class FoDClientCredentialOptions implements IFoDClientCredentials {
        @Option(names = {"--client-id"}, required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.medium, description = "FOD CLIENT ID")
        @Getter private String clientId;
        @Option(names = {"--client-secret"}, required = true, interactive = true, arity = "0..1", echo = false) 
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "FOD CLIENT SECRET")
        @Getter private String clientSecret;
    }

    public UserCredentialOptions getUserCredentialOptions() {
        return Optional.ofNullable(authOptions)
                .map(FoDAuthOptions::getCredentialOptions)
                .map(FoDCredentialOptions::getUserCredentialOptions)
                .orElse(null);
    }
    
    public FoDClientCredentialOptions getClientCredentialOptions() {
        return Optional.ofNullable(authOptions)
                .map(FoDAuthOptions::getCredentialOptions)
                .map(FoDCredentialOptions::getClientCredentialOptions)
                .orElse(null);
    }

    public final boolean hasUserCredentials() {
        return getUserCredentialOptions()!=null;
    }

    public final BasicFoDUserCredentials getUserCredentials() {
        var u = getUserCredentialOptions();
        var t = Optional.ofNullable(authOptions).map(FoDAuthOptions::getTenant).orElse(null);
        if ( u==null || StringUtils.isBlank(t) || StringUtils.isBlank(u.getUser()) || u.getPassword()==null ) {
            throw new FcliSimpleException("--tenant, --user and --password must all be specified for user credential authentication");
        }
        return BasicFoDUserCredentials.builder().tenant(t).user(u.getUser()).password(u.getPassword()).build();
    }
    
    public final boolean hasClientCredentials() {
        FoDClientCredentialOptions clientCredentialOptions = getClientCredentialOptions();
        return clientCredentialOptions!=null
                && StringUtils.isNotBlank(clientCredentialOptions.getClientId())
                && StringUtils.isNotBlank(clientCredentialOptions.getClientSecret());
    }
    
    @Command
    public static final class FoDUrlConfigOptions extends UrlConfigOptions {
        @Override @SneakyThrows
        public String getUrl() {
            return FoDProductHelper.INSTANCE.getApiUrl(super.getUrl());
        }
        
        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }

    /**
     * Basic immutable FoD user credentials with builder pattern.
     */
    public static final class BasicFoDUserCredentials implements IFoDUserCredentials {
        private final String tenant;
        private final String user;
        private final char[] password;
        private BasicFoDUserCredentials(Builder b) {
            this.tenant = b.tenant;
            this.user = b.user;
            this.password = b.password;
        }
        public static Builder builder() { return new Builder(); }
        @Override public String getTenant() { return tenant; }
        @Override public String getUser() { return user; }
        @Override public char[] getPassword() { return password; }
        public static final class Builder {
            private String tenant; private String user; private char[] password;
            public Builder tenant(String tenant){ this.tenant=tenant; return this; }
            public Builder user(String user){ this.user=user; return this; }
            public Builder password(char[] password){ this.password=password; return this; }
            public BasicFoDUserCredentials build(){
                if ( StringUtils.isBlank(tenant) || StringUtils.isBlank(user) || password==null || password.length==0 ) {
                    throw new FcliSimpleException("--tenant, --user and --password must all be specified for user credential authentication");
                }
                return new BasicFoDUserCredentials(this);
            }
        }
    }
}
