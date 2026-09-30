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
import java.util.Optional;

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
    String sourceTypeId();

    /**
     * Credential check to run after transport stages, if any.
     * Bare URL returns empty (no credential stage).
     */
    Optional<CredentialRequest> credentialRequest();

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

    /** Product credential stage (not transport stages). */
    sealed interface CredentialRequest {
        void accept(Visitor visitor);

        /**
         * Exhaustive dispatch over credential modes (Java 17-friendly; no pattern switch).
         */
        interface Visitor {
            void visitToken(Token token);

            void visitAdmin(Admin admin);
        }

        /**
         * User token validation. {@code token} is non-null for {@code --url --token};
         * session-sourced tokens may be null/blank (corrupted store) and fail optionally.
         */
        record Token(String url, String token) implements CredentialRequest {
            public Token {
                Objects.requireNonNull(url, "url");
            }

            @Override
            public void accept(Visitor visitor) {
                visitor.visitToken(this);
            }
        }

        record Admin(AviatorAdminConfigDescriptor descriptor) implements CredentialRequest {
            public Admin {
                Objects.requireNonNull(descriptor, "descriptor");
            }

            @Override
            public void accept(Visitor visitor) {
                visitor.visitAdmin(this);
            }
        }
    }

    record UrlOnly(String url) implements AviatorConnectionDiagnoseSource {
        public UrlOnly {
            Objects.requireNonNull(url, "url");
        }

        @Override
        public String sourceTypeId() {
            return "url";
        }

        @Override
        public Optional<CredentialRequest> credentialRequest() {
            return Optional.empty();
        }
    }

    record UrlAndToken(String url, String token) implements AviatorConnectionDiagnoseSource {
        public UrlAndToken {
            Objects.requireNonNull(url, "url");
            Objects.requireNonNull(token, "token");
            if (token.isBlank()) {
                throw new IllegalArgumentException("token must not be blank");
            }
        }

        @Override
        public String sourceTypeId() {
            return "url-token";
        }

        @Override
        public Optional<CredentialRequest> credentialRequest() {
            return Optional.of(new CredentialRequest.Token(url, token));
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
        public String sourceTypeId() {
            return "user-session";
        }

        @Override
        public Optional<CredentialRequest> credentialRequest() {
            return Optional.of(new CredentialRequest.Token(url(), descriptor.getAviatorToken()));
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
        public String sourceTypeId() {
            return "admin-config";
        }

        @Override
        public Optional<CredentialRequest> credentialRequest() {
            return Optional.of(new CredentialRequest.Admin(descriptor));
        }
    }
}
