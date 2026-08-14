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
package com.fortify.cli.aviator._common.session.user.helper;

import java.util.Locale;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import com.fortify.cli.common.exception.FcliSimpleException;

/**
 * Shared validation for Aviator user token option sources ({@code file:}/{@code string:}/{@code env:}).
 * Used by required (session login) and optional (connection diagnose) token mixins.
 */
public final class AviatorUserTokenTextResolver {
    private AviatorUserTokenTextResolver() {}

    /**
     * Resolve a required token. {@code resolvedTokenSupplier} is invoked after the source is present.
     *
     * @param textSource raw option value (e.g. {@code env:MY_TOKEN})
     * @param resolvedTokenSupplier typically {@code AbstractTextResolverMixin#getText}
     * @return non-blank token text
     */
    public static String resolveRequired(String textSource, Supplier<String> resolvedTokenSupplier) {
        rejectUrlPrefix(textSource);
        var resolvedToken = resolvedTokenSupplier.get();
        if (StringUtils.isBlank(resolvedToken)) {
            throw new FcliSimpleException("Resolved token value for --token option is blank or empty.");
        }
        return resolvedToken;
    }

    /**
     * Resolve an optional token. Returns {@code null} when {@code textSource} was not provided.
     */
    public static String resolveOptional(String textSource, Supplier<String> resolvedTokenSupplier) {
        if (StringUtils.isBlank(textSource)) {
            return null;
        }
        return resolveRequired(textSource, resolvedTokenSupplier);
    }

    private static void rejectUrlPrefix(String textSource) {
        if (textSource != null && textSource.toLowerCase(Locale.ROOT).startsWith("url:")) {
            throw new FcliSimpleException("Providing Aviator tokens via URL ('url:' prefix) is not supported");
        }
    }
}
