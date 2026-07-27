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
import picocli.CommandLine.Option;

/**
 * URL-based diagnose source: required {@code --url}, optional {@code --token} for direct token validation.
 */
@Getter
public class AviatorConnectionDiagnoseUrlSourceArgGroup extends AbstractTextResolverMixin {
    @Option(names = {"--url"}, required = true, order = 1)
    @MaskValue(sensitivity = LogSensitivityLevel.low, description = "AVIATOR HOST NAME", pattern = MaskValue.URL_HOSTNAME_PATTERN)
    private String url;

    @Option(names = {"--token", "-t"}, descriptionKey = "fcli.aviator.connection.diagnose.token",
        paramLabel = "source", required = false, order = 2)
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "AVIATOR TOKEN")
    private String tokenSource;

    @Override
    public String getTextSource() {
        return tokenSource;
    }

    public String getTokenOrNull() {
        return AviatorUserTokenTextResolver.resolveOptional(getTextSource(), this::getText);
    }
}
