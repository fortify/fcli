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
package com.fortify.cli.common.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import com.fortify.cli.common.cli.util.WindowsFileConverter;
import com.fortify.cli.common.cli.util.WindowsPathConverter;
import com.fortify.cli.common.exception.FcliSimpleException;

class WindowsPathValidatorTest {
    private static final String CORRUPTED_PATH = "C:\\temp\\?\\source";
    private static final String REPLACEMENT_PATH = "C:\\temp\\\uFFFD\\source";

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void rejectsQuestionMarkPath() {
        assertTrue(WindowsPathValidator.hasUnsupportedCharacters(CORRUPTED_PATH));
        assertThrows(FcliSimpleException.class, () -> new WindowsPathConverter().convert(CORRUPTED_PATH));
        assertThrows(FcliSimpleException.class, () -> new WindowsFileConverter().convert(CORRUPTED_PATH));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void rejectsReplacementCharacterPath() {
        assertTrue(WindowsPathValidator.hasUnsupportedCharacters(REPLACEMENT_PATH));
        FcliSimpleException exception = assertThrows(
                FcliSimpleException.class, () -> WindowsPathValidator.validate("--source-dir", REPLACEMENT_PATH));
        assertTrue(exception.getMessage().contains("Windows command-line decoding"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void acceptsUnicodePathWithoutCorruptionMarkers() {
        assertDoesNotThrow(() -> WindowsPathValidator.validate("--source-dir", "C:\\temp\\Fortify-test\\source"));
    }
}
