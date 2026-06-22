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
package com.fortify.cli.ai_assist.mcp.helper.http;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.log.LogMaskHelper;
import com.fortify.cli.common.log.LogMaskSource;
import com.fortify.cli.common.log.LogSensitivityLevel;

/**
 * Parsed representation of an X-AUTH-SSC or X-AUTH-FOD header.
 *
 * <p>Calling {@link #registerCredentials()} routes all credential values through
 * {@link LogMaskHelper#registerValue} (which in turn routes to
 * {@link com.fortify.cli.common.log.LogMaskContext#activeContext()}).  This must be called
 * while a pre-scope capture context is active so that credential masking is scoped to the
 * current isolation scope rather than accumulated globally.</p>
 */
record ParsedAuthorization(
        MCPServerHttpConfig.Product product,
        String sscToken,
        String scSastClientAuthToken,
        String fodClientId,
        String fodClientSecret,
        String fodTenant,
        String fodUser,
        String fodPat) {

    void registerCredentials() {
        switch ( product ) {
            case ssc -> {
                register(LogSensitivityLevel.high, "SSC TOKEN", sscToken);
                register(LogSensitivityLevel.high, "SSC SC-SAST TOKEN", scSastClientAuthToken);
            }
            case fod -> {
                register(LogSensitivityLevel.medium, "FOD CLIENT ID", fodClientId);
                register(LogSensitivityLevel.high, "FOD CLIENT SECRET", fodClientSecret);
                register(LogSensitivityLevel.low, "FOD TENANT", fodTenant);
                register(LogSensitivityLevel.medium, "USER", fodUser);
                register(LogSensitivityLevel.high, "PASSWORD", fodPat);
            }
        }
    }

    private static void register(LogSensitivityLevel sensitivity, String description, String value) {
        if ( StringUtils.isNotBlank(value) ) {
            LogMaskHelper.INSTANCE.registerValue(sensitivity, LogMaskSource.HTTP_AUTH_HEADER, description, value, "");
        }
    }
}
