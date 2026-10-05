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
package com.fortify.cli.aviator.connection.cli.mixin;

import com.fortify.cli.aviator._common.session.user.helper.AviatorUserTokenTextResolver;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import lombok.Getter;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Option;

/**
 * URL-based diagnose source: required {@code --url}, optional {@code --token} for direct token validation.
 * <p>
 * Token resolution is a nested {@link AbstractTextResolverMixin} so class-level
 * {@link MaskValue} applies only to the resolved token (not the URL host).
 */
@Getter
public class AviatorConnectionDiagnoseUrlSourceArgGroup {
    @Option(names = {"--url"}, required = true, order = 1)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "AVIATOR HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    private String url;

    /** Present only when {@code --token} is supplied (optional ArgGroup). */
    @ArgGroup(exclusive = false, multiplicity = "0..1", order = 2)
    private TokenSource tokenSource;

    public String getTokenOrNull() {
        return tokenSource == null ? null : tokenSource.getTokenOrNull();
    }

    /**
     * Optional token text source. Class-level {@link MaskValue} registers the resolved token
     * for log masking ({@link AbstractTextResolverMixin#getText()}); field-level masks the raw option.
     */
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "AVIATOR TOKEN")
    public static final class TokenSource extends AbstractTextResolverMixin {
        @Option(names = {"--token", "-t"}, paramLabel = "source", required = true, order = 2)
        @MaskValue(sensitivity = LogSensitivityLevel.high, description = "AVIATOR TOKEN")
        private String textSource;

        @Override
        public String getTextSource() {
            return textSource;
        }

        String getTokenOrNull() {
            return AviatorUserTokenTextResolver.resolveOptional(getTextSource(), this::getText);
        }
    }
}
