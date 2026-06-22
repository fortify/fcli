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
package com.fortify.cli.common.session.cli.mixin;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fortify.cli.common.session.helper.ISessionDescriptor;

import picocli.CommandLine.Option;

/**
 * Base mixin for session validation. Subclasses provide product-specific
 * validation logic. Declare as a {@code @Mixin} on session list commands
 * that support live validation (currently FoD and SSC only).
 *
 * @param <D> the session descriptor type
 */
public abstract class AbstractSessionValidatorMixin<D extends ISessionDescriptor> {
    @Option(names = {"--validate"}, required = false, descriptionKey = "fcli.session.validate")
    private boolean validate;

    public boolean isValidationEnabled() {
        return validate;
    }

    /**
     * Whether to perform live server-side validation for sessions that are already
     * considered expired based on locally cached data. Return {@code false} when
     * expired sessions can never be valid again (e.g. FoD access tokens);
     * return {@code true} (default) when a server may have extended the lifetime
     * of the token since the last login (e.g. SSC).
     */
    public boolean shouldValidateExpiredSessions() {
        return true;
    }

    /**
     * Enrich {@code sessionNode} with server-side status for the given session.
     * The descriptor may be {@code null} when the session file is unreadable.
     * Implementations should mutate {@code sessionNode} in-place (e.g. set
     * {@code expired} / {@code expires} fields). If the descriptor was mutated
     * and should be persisted, return {@code true}; otherwise return {@code false}.
     *
     * @param sessionName  name of the session being validated
     * @param descriptor   session descriptor loaded from disk, or {@code null}
     * @param sessionNode  the JSON object node that will be included in the output
     * @return {@code true} if the descriptor was updated and should be saved
     */
    public abstract boolean updateSessionNode(String sessionName, D descriptor, ObjectNode sessionNode);
}
