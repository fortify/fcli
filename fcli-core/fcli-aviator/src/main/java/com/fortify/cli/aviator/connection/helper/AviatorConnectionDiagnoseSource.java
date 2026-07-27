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

import java.util.Objects;

import com.fortify.cli.aviator._common.config.admin.helper.AviatorAdminConfigDescriptor;
import com.fortify.cli.aviator._common.session.user.helper.AviatorUserSessionDescriptor;

/**
 * Domain source for Aviator connection diagnostics (no Picocli types).
 * <p>
 * Sealed variants make exclusive modes unrepresentable as dual-null bags:
 * bare URL, URL+token, saved user session, or admin config.
 */
public sealed interface AviatorConnectionDiagnoseSource {

    String url();

    /** Machine-readable source id for endpoint evidence ({@code sourceType}). */
    SourceType sourceType();

    default String type() {
        return sourceType().id();
    }

    static AviatorConnectionDiagnoseSource fromUrl(String url) {
        return new UrlOnly(url);
    }

    static AviatorConnectionDiagnoseSource fromUrlAndToken(String url, String token) {
        return new UrlAndToken(url, token);
    }

    static AviatorConnectionDiagnoseSource fromUserSession(AviatorUserSessionDescriptor descriptor) {
        return new UserSession(descriptor);
    }

    static AviatorConnectionDiagnoseSource fromAdminConfig(AviatorAdminConfigDescriptor descriptor) {
        return new AdminConfig(descriptor);
    }

    /** Wire ids for endpoint evidence {@code sourceType}. */
    enum SourceType {
        URL("url"),
        URL_TOKEN("url-token"),
        USER_SESSION("user-session"),
        ADMIN_CONFIG("admin-config");

        private final String id;

        SourceType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    record UrlOnly(String url) implements AviatorConnectionDiagnoseSource {
        public UrlOnly {
            Objects.requireNonNull(url, "url");
        }

        @Override
        public SourceType sourceType() {
            return SourceType.URL;
        }
    }

    record UrlAndToken(String url, String token) implements AviatorConnectionDiagnoseSource {
        public UrlAndToken {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(token, "token");
        }

        @Override
        public SourceType sourceType() {
            return SourceType.URL_TOKEN;
        }
    }

    record UserSession(AviatorUserSessionDescriptor descriptor) implements AviatorConnectionDiagnoseSource {
        public UserSession {
            Objects.requireNonNull(descriptor, "descriptor");
        }

        @Override
        public String url() {
            return descriptor.getAviatorUrl();
        }

        @Override
        public SourceType sourceType() {
            return SourceType.USER_SESSION;
        }
    }

    record AdminConfig(AviatorAdminConfigDescriptor descriptor) implements AviatorConnectionDiagnoseSource {
        public AdminConfig {
            Objects.requireNonNull(descriptor, "descriptor");
        }

        @Override
        public String url() {
            return descriptor.getAviatorUrl();
        }

        @Override
        public SourceType sourceType() {
            return SourceType.ADMIN_CONFIG;
        }
    }
}
