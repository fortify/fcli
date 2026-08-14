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
package com.fortify.cli.aviator._common.session.user.cli.mixin;

import com.fortify.cli.aviator._common.session.user.helper.AviatorUserTokenTextResolver;
import com.fortify.cli.common.cli.mixin.CommonOptionMixins.AbstractTextResolverMixin;
import com.fortify.cli.common.log.LogSensitivityLevel;
import com.fortify.cli.common.log.MaskValue;

import picocli.CommandLine.Option;

/**
 * Mixin for resolving an Aviator user token from various sources (direct string, file, environment variable).
 */
public class AviatorUserTokenResolverMixin extends AbstractTextResolverMixin {
    @Option(names = {"--token", "-t"}, descriptionKey = "fcli.aviator.session.login.token", paramLabel = "source", required = true, order = 1)
    @MaskValue(sensitivity = LogSensitivityLevel.high, description = "AVIATOR TOKEN")
    private String textSource;

    @Override
    public String getTextSource() {
        return textSource;
    }

    /**
     * @return the resolved Aviator user token string
     */
    public String getToken() {
        return AviatorUserTokenTextResolver.resolveRequired(getTextSource(), this::getText);
    }
}
