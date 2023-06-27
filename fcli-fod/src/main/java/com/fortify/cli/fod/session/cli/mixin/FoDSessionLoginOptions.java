/*******************************************************************************
 * Copyright 2021, 2023 Open Text.
 *
 * The only warranties for products and services of Open Text 
 * and its affiliates and licensors (“Open Text”) are as may 
 * be set forth in the express warranty statements accompanying 
 * such products and services. Nothing herein should be construed 
 * as constituting an additional warranty. Open Text shall not be 
 * liable for technical or editorial errors or omissions contained 
 * herein. The information contained herein is subject to change 
 * without notice.
 *******************************************************************************/
package com.fortify.cli.fod.session.cli.mixin;

import java.util.Optional;

import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.fod.session.helper.oauth.IFoDClientCredentials;
import com.fortify.cli.fod.session.helper.oauth.IFoDUserCredentials;

import io.micronaut.core.util.StringUtils;
import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

public class FoDSessionLoginOptions {
    @ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.fod-connection-options.heading", order = 1)
    @Getter private UrlConfigOptions urlConfigOptions = new UrlConfigOptions();
    
    @ArgGroup(exclusive = false, multiplicity = "1", headingKey = "arggroup.fod-authentication-options.heading", order = 2)
    @Getter private FoDAuthOptions authOptions = new FoDAuthOptions();
    
    public String[] getScopes() {
        return new String[]{"api-tenant"}; // TODO make scopes configurable
    }
    
    public static class FoDAuthOptions {
        @ArgGroup(exclusive = true, multiplicity = "1", order = 3)
        @Getter private FoDCredentialOptions credentialOptions = new FoDCredentialOptions();
    }
    
    public static class FoDCredentialOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1) 
        @Getter private FoDUserCredentialOptions userCredentialOptions = new FoDUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2) 
        @Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }
    
    public static class FoDUserCredentialOptions extends UserCredentialOptions implements IFoDUserCredentials {
        @Option(names = {"--tenant"}, required = true) 
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
                && StringUtils.isNotEmpty(userCredentialOptions.getTenant())
                && StringUtils.isNotEmpty(userCredentialOptions.getUser())
                && userCredentialOptions.getPassword()!=null;
    }
    
    public final boolean hasClientCredentials() {
        FoDClientCredentialOptions clientCredentialOptions = getClientCredentialOptions();
        return clientCredentialOptions!=null
                && StringUtils.isNotEmpty(clientCredentialOptions.getClientId())
                && StringUtils.isNotEmpty(clientCredentialOptions.getClientSecret());
    }
}
