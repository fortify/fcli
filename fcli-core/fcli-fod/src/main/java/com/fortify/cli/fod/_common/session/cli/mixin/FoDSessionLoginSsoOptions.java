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

import java.net.URI;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;
import com.fortify.cli.common.rest.cli.mixin.ConnectionConfigOptions;
import com.fortify.cli.common.rest.unirest.config.IUrlConfig;
import com.fortify.cli.common.rest.unirest.config.UrlConfig;
import com.fortify.cli.common.util.DateTimePeriodHelper;
import com.fortify.cli.common.util.DateTimePeriodHelper.Period;
import com.fortify.cli.fod._common.rest.helper.FoDProductHelper;

import lombok.Getter;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

/**
 * Options for 'fcli fod session login-sso'. The FoD API URL is derived from
 * the SAML IdP URL, so no separate --url option is required.
 */
public class FoDSessionLoginSsoOptions {
    private static final DateTimePeriodHelper PERIOD_HELPER = DateTimePeriodHelper.byRange(Period.SECONDS, Period.MINUTES);

    @Mixin @Getter private FoDSsoConnectionOptions connectionOptions = new FoDSsoConnectionOptions();

    @Option(names = {"--sso-url"}, required = true, order = 1)
    @MaskValue(sensitivity = LogSensitivityLevel.medium, description = "FOD SSO URL")
    @Getter private String ssoUrl;

    @Option(names = {"--url"}, order = 1)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    @Getter private String url;

    @Option(names = {"-t", "--tenant"}, order = 2)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "FOD TENANT")
    @Getter private String tenant;

    @Option(names = "--sso-timeout", defaultValue = "5m", order = 4)
    private String ssoTimeout;

    @Option(names = "--browser", paramLabel = "<chrome|edge>", order = 5)
    @Getter private String browser;

    @Option(names = "--browser-path", paramLabel = "<file>", order = 6)
    @Getter private String browserPath;

    @Option(names = "--browser-profile", paramLabel = "<dir>", order = 7)
    @Getter private String browserProfile;

    @Option(names = "--saml-assertion-file", paramLabel = "<file|->", order = 8)
    @Getter private String samlAssertionFile;

    public final boolean hasSamlAssertionFile() {
        return StringUtils.isNotBlank(samlAssertionFile);
    }

    public final Duration getSsoTimeout() {
        return Duration.ofMillis(PERIOD_HELPER.parsePeriodToMillis(ssoTimeout));
    }

    public final URI getSsoUri() {
        try {
            var uri = URI.create(ssoUrl.trim());
            boolean valid = uri.getHost() != null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()));
            if ( !valid ) { throw new IllegalArgumentException(); }
            return uri;
        } catch ( IllegalArgumentException e ) {
            throw new FcliSimpleException("Invalid --sso-url; an absolute http(s) URL is required");
        }
    }

    public final IUrlConfig getUrlConfig() {
        return UrlConfig.builderFromConnectionConfig(connectionOptions)
                .url(StringUtils.isNotBlank(url) ? StringUtils.stripEnd(url.trim(), "/") : deriveApiUrl())
                .build();
    }

    private String deriveApiUrl() {
        var uri = getSsoUri();
        var baseUrl = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
        if ( uri.getPort() > 0 ) { baseUrl.append(':').append(uri.getPort()); }
        return FoDProductHelper.INSTANCE.getApiUrl(baseUrl.toString());
    }

    public final boolean hasTenant() {
        return StringUtils.isNotBlank(tenant);
    }

    @Command
    public static final class FoDSsoConnectionOptions extends ConnectionConfigOptions {
        @Override
        protected int getDefaultSocketTimeoutInMillis() {
            return 600000;
        }
    }
}
