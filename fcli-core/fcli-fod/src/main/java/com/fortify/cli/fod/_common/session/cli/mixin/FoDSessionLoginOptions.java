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
package com.fortify.cli.fod._common.session.cli.mixin;

import java.util.Optional;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.StringUtils;
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
    }
    
    public static class FoDCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private FoDUserCredentialOptions userCredentialOptions = new FoDUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }
    
    public static class FoDUserCredentialOptions extends UserCredentialOptions implements IFoDUserCredentials {
        @Option(names = {"-t", "--tenant"}, required = true) 
        @Getter private String tenant;
    }
    
    public static class FoDClientCredentialOptions implements IFoDClientCredentials {
        @Option(names = {"--client-id"}, required = true) 
        @Getter private String clientId;
        @Option(names = {"--client-secret"}, required = true, interactive = true, arity = "0..1", echo = false) 
        @Getter private String clientSecret;
    }

    public FoDUserCredentialOptions getUserCredentialOptions() {
        return Optional.ofNullable(authOptions).map(FoDAuthOptions::getCredentialOptions).map(FoDCredentialOptions::getUserCredentialOptions).orElse(null);
    }
    
    public FoDClientCredentialOptions getClientCredentialOptions() {
        return Optional.ofNullable(authOptions).map(FoDAuthOptions::getCredentialOptions).map(FoDCredentialOptions::getClientCredentialOptions).orElse(null);
    }
    
    public final boolean hasUserCredentialsConfig() {
        FoDUserCredentialOptions userCredentialOptions = getUserCredentialOptions();
        return userCredentialOptions!=null 
                && StringUtils.isNotBlank(userCredentialOptions.getTenant())
                && StringUtils.isNotBlank(userCredentialOptions.getUser())
                && userCredentialOptions.getPassword()!=null;
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
}
