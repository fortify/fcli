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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorUserTokenTextResolverTest {

    @Test
    void resolveOptionalReturnsNullWhenSourceBlank() {
        assertNull(AviatorUserTokenTextResolver.resolveOptional(null, () -> "x"));
        assertNull(AviatorUserTokenTextResolver.resolveOptional("  ", () -> "x"));
    }

    @Test
    void resolveRequiredRejectsUrlPrefix() {
        assertThrows(FcliSimpleException.class,
            () -> AviatorUserTokenTextResolver.resolveRequired("url:http://evil", () -> "token"));
        assertThrows(FcliSimpleException.class,
            () -> AviatorUserTokenTextResolver.resolveRequired("URL:http://evil", () -> "token"));
    }

    @Test
    void resolveOptionalRejectsUrlPrefix() {
        assertThrows(FcliSimpleException.class,
            () -> AviatorUserTokenTextResolver.resolveOptional("url:http://evil", () -> "token"));
        assertThrows(FcliSimpleException.class,
            () -> AviatorUserTokenTextResolver.resolveOptional("URL:http://evil", () -> "token"));
    }

    @Test
    void resolveRequiredRejectsBlankToken() {
        assertThrows(FcliSimpleException.class,
            () -> AviatorUserTokenTextResolver.resolveRequired("string: ", () -> "  "));
    }

    @Test
    void resolveRequiredReturnsToken() {
        assertEquals("abc", AviatorUserTokenTextResolver.resolveRequired("string:abc", () -> "abc"));
    }
}
