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
package com.fortify.cli.aviator.app.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fortify.aviator.application.Application;
import com.fortify.cli.aviator._common.exception.AviatorSimpleException;

class AviatorAppHelperTest {
    private static Application app(long id, String name) {
        return Application.newBuilder().setId(id).setName(name).build();
    }

    private final List<Application> apps = List.of(app(1, "first"), app(2, "second"), app(3, "2024"), app(2024, "other"));

    @Test
    void isNumericAcceptsOnlyDigits() {
        assertTrue(AviatorAppHelper.isNumeric("123"));
        assertFalse(AviatorAppHelper.isNumeric(null));
        assertFalse(AviatorAppHelper.isNumeric(""));
        assertFalse(AviatorAppHelper.isNumeric("-1"));
        assertFalse(AviatorAppHelper.isNumeric("12a"));
        assertFalse(AviatorAppHelper.isNumeric("1234567890123456789"));
    }

    @Test
    void numericValuesAreReturnedAsIdWithoutContactingServer() {
        // null client and config descriptor prove that no server call is made for numeric values
        assertEquals("123", AviatorAppHelper.resolveApplicationId(null, null, "123"));
        assertEquals("2024", AviatorAppHelper.resolveApplicationId(null, null, "2024"));
    }

    @Test
    void failsIfBlank() {
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationId(null, null, " "));
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationId(null, null, null));
    }

    @Test
    void resolvesNameToId() {
        assertEquals("2", AviatorAppHelper.resolveApplicationIdByName(apps, "second"));
    }

    @Test
    void failsIfNameNotFound() {
        var e = assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationIdByName(apps, "missing"));
        assertTrue(e.getMessage().contains("missing"));
    }

    @Test
    void failsIfNameIsAmbiguous() {
        var duplicates = List.of(app(1, "dup"), app(2, "dup"));
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationIdByName(duplicates, "dup"));
    }

    @Test
    void namesAreCaseSensitive() {
        assertThrows(AviatorSimpleException.class, () -> AviatorAppHelper.resolveApplicationIdByName(apps, "FIRST"));
    }
}
