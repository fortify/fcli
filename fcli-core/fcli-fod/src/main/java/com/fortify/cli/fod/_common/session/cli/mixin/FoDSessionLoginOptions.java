/*
 * Copyright 2021-2026 Open Text.
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

import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.cli.mixin.UrlConfigOptions;
import com.fortify.cli.common.session.cli.mixin.UserCredentialOptions;
import com.fortify.cli.common.util.DisableTest;
import com.fortify.cli.common.util.DisableTest.TestType;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDClientCredentials;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDUserAuthCode;
import com.fortify.cli.fod._common.session.helper.oauth.IFoDUserCredentials;
import com.fortify.cli.fod._common.session.helper.oauth.impl.BasicFoDUserAuthCode;
import com.fortify.cli.fod._common.session.helper.oauth.impl.BasicFoDUserCredentials;

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

    public static class FoDUserCredentialOptions extends UserCredentialOptions {
        @Option(names = {"-t", "--tenant"}, required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TENANT")
        @Getter private String tenant;
        @Option(names = {"--code", "-c" }, paramLabel = "<code>", arity = "0..1", interactive = true, echo = false)
        @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TOTP/MFA CODE")
        @Getter private String securityCode;
        @Option(names = {"--totp"}, arity = "0..1", fallbackValue = "true", paramLabel = "<code>")
        @DisableTest(TestType.OPT_ARITY_PRESENT) // arity needed for optional-value flag pattern
        @Getter private String totp;
    }

    public static class FoDClientCredentialOptions implements IFoDClientCredentials {
        @Option(names = {"--client-id"}, required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.medium, description = "FOD CLIENT ID")
        @Getter private String clientId;
        @Option(names = {"--client-secret"}, required = true, interactive = true, arity = "0..1", echo = false) 
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "FOD CLIENT SECRET")
        @Getter private String clientSecret;
    }

    public FoDUserCredentialOptions getUserCredentialOptions() {
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
        var userCredentialOptions = getUserCredentialOptions();
        return userCredentialOptions!=null
                && StringUtils.isNotBlank(userCredentialOptions.getTenant())
                && StringUtils.isNotBlank(userCredentialOptions.getUser())
                && userCredentialOptions.getPassword()!=null
                && userCredentialOptions.getPassword().length > 0;
    }

    public final IFoDUserCredentials getUserCredentials() {
        var u = getUserCredentialOptions();
        return BasicFoDUserCredentials.builder()
                .tenant(u.getTenant())
                .user(u.getUser())
                .password(u.getPassword())
                .build();
    }

    public final boolean hasClientCredentials() {
        FoDClientCredentialOptions clientCredentialOptions = getClientCredentialOptions();
        return clientCredentialOptions!=null
                && StringUtils.isNotBlank(clientCredentialOptions.getClientId())
                && StringUtils.isNotBlank(clientCredentialOptions.getClientSecret());
    }

    public boolean hasSecurityCode() {
        var userCred = getUserCredentialOptions();
        if (userCred == null) { return false; }
        var totp = userCred.getTotp();
        return (StringUtils.isNotBlank(totp) && !"true".equals(totp))
            || StringUtils.isNotBlank(userCred.getSecurityCode());
    }

    public String getSecurityCode() {
        var userCred = getUserCredentialOptions();
        return userCred != null ? userCred.getSecurityCode() : null;
    }

    public boolean isTotp() {
        var userCred = getUserCredentialOptions();
        return userCred != null && userCred.getTotp() != null;
    }

    private String resolveSecurityCode(FoDUserCredentialOptions u) {
        var totp = u.getTotp();
        return (totp != null && !"true".equals(totp)) ? totp : u.getSecurityCode();
    }

    public IFoDUserAuthCode getAuthCode() {
        var u = getUserCredentialOptions();
        var code = u != null ? resolveSecurityCode(u) : null;
        if (StringUtils.isBlank(code)) { return null; }
        return BasicFoDUserAuthCode.builder()
                .securityCode(code)
                .isTotp(u.getTotp() != null)
                .build();
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
