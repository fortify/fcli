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
package com.fortify.cli.aviator.connection.helper;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;

/**
 * Domain source for Aviator connection diagnostics (no Picocli types).
 */
public record AviatorConnectionDiagnoseSource(
        String type,
        String url,
        AviatorUserSessionDescriptor userSessionDescriptor,
        AviatorAdminConfigDescriptor adminConfigDescriptor,
        String rawToken) {

    public static AviatorConnectionDiagnoseSource fromUrl(String url) {
        return new AviatorConnectionDiagnoseSource("url", url, null, null, null);
    }

    public static AviatorConnectionDiagnoseSource fromUrlAndToken(String url, String token) {
        return new AviatorConnectionDiagnoseSource("url-token", url, null, null, token);
    }

    public static AviatorConnectionDiagnoseSource fromUserSession(AviatorUserSessionDescriptor descriptor) {
        return new AviatorConnectionDiagnoseSource("user-session", descriptor.getAviatorUrl(), descriptor, null, null);
    }

    public static AviatorConnectionDiagnoseSource fromAdminConfig(AviatorAdminConfigDescriptor descriptor) {
        return new AviatorConnectionDiagnoseSource("admin-config", descriptor.getAviatorUrl(), null, descriptor, null);
    }

    public boolean hasAdminConfig() {
        return adminConfigDescriptor != null;
    }

    public boolean hasUserToken() {
        return userSessionDescriptor != null || rawToken != null;
    }

    public boolean hasCredentials() {
        return hasAdminConfig() || hasUserToken();
    }
}
