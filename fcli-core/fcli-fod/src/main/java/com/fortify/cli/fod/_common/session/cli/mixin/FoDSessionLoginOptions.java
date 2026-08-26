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

import com.fortify.cli.common.exception.FcliSimpleException;
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
        @Getter private FoDUserCredentialWithMfaOptions userCredentialWithMfaOptions = new FoDUserCredentialWithMfaOptions();
        @ArgGroup(exclusive = false, multiplicity = "1", order = 2)
        @Getter private FoDClientCredentialOptions clientCredentialOptions = new FoDClientCredentialOptions();
    }

    public static class FoDUserCredentialWithMfaOptions {
        @ArgGroup(exclusive = false, multiplicity = "1", order = 1)
        @Getter private FoDUserCredentialOptions userCredentialOptions = new FoDUserCredentialOptions();
        @ArgGroup(exclusive = false, multiplicity = "0..1", order = 2)
        @Getter private FoDMfaOptions mfaOptions = new FoDMfaOptions();
    }

    public static class FoDMfaOptions {
        // Marker value picocli assigns when the option is given as a bare flag (no inline value).
        private static final String FLAG = "true";

        // Not interactive: prompting is handled in computeMfaCode() below, since whether/what to
        // prompt for depends on both --code and --totp together (see computeMfaCode() javadoc).
        @Option(names = {"--code", "-c" }, paramLabel = "<code>", arity = "0..1", fallbackValue = FLAG)
        @DisableTest(TestType.OPT_ARITY_PRESENT) // arity needed for optional-value flag pattern
        @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TOTP/MFA CODE")
        @Getter private String securityCode;
        @Option(names = {"--totp"}, arity = "0..1", fallbackValue = FLAG, paramLabel = "<totp>")
        @DisableTest(TestType.OPT_ARITY_PRESENT) // arity needed for optional-value flag pattern
        @Getter private String totp;

        /** Whether --totp was specified in any form (bare flag or with a value). */
        public boolean isTotp() {
            return totp != null;
        }

        /**
         * Resolves the effective MFA code, supporting both the legacy {@code --code <code> --totp}
         * and the new {@code --totp <code>} usage. An explicit value on either option is used as-is;
         * otherwise, if given as a bare flag, prompts interactively for the code, preferring --totp's
         * prompt over --code's if both are given bare (--totp implies the code is TOTP, not email/SMS).
         * Result is cached, so the prompt (if any) only happens once.
         */
        @Getter(lazy = true) private final String mfaCode = computeMfaCode();

        private String computeMfaCode() {
            var explicitTotp = valueOrNull(totp);
            var explicitCode = valueOrNull(securityCode);
            if (explicitTotp != null) { return explicitTotp; }
            if (explicitCode != null) { return explicitCode; }
            if (FLAG.equals(totp)) { return promptFor("TOTP code: "); }
            if (FLAG.equals(securityCode)) {
                return promptFor("MFA security code (from email/SMS; use 'fcli fod session request-mfa-code' to request one): ");
            }
            return null;
        }

        private static String valueOrNull(String value) {
            return value == null || FLAG.equals(value) ? null : value;
        }

        private String promptFor(String prompt) {
            var console = System.console();
            if (console == null) {
                throw new FcliSimpleException("No console available to prompt for MFA code; specify --totp <code> or --code <code> instead");
            }
            return console.readLine(prompt);
        }
    }

    public static class FoDUserCredentialOptions extends UserCredentialOptions implements IFoDUserCredentials {
        @Option(names = {"-t", "--tenant"}, required = true)
        @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TENANT")
        @Getter private String tenant;
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
                .map(FoDCredentialOptions::getUserCredentialWithMfaOptions)
                .map(FoDUserCredentialWithMfaOptions::getUserCredentialOptions)
                .orElse(null);
    }

    private FoDMfaOptions getMfaOptions() {
        return Optional.ofNullable(authOptions)
                .map(FoDAuthOptions::getCredentialOptions)
                .map(FoDCredentialOptions::getUserCredentialWithMfaOptions)
                .map(FoDUserCredentialWithMfaOptions::getMfaOptions)
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
        var mfaOptions = getMfaOptions();
        return mfaOptions != null && StringUtils.isNotBlank(mfaOptions.getMfaCode());
    }

    public IFoDUserAuthCode getAuthCode() {
        var mfaOptions = getMfaOptions();
        if (mfaOptions == null || StringUtils.isBlank(mfaOptions.getMfaCode())) { return null; }
        return BasicFoDUserAuthCode.builder()
                .securityCode(mfaOptions.getMfaCode())
                .isTotp(mfaOptions.isTotp())
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
