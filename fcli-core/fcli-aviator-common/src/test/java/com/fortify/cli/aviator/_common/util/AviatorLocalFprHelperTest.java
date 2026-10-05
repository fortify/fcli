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
package com.fortify.cli.aviator._common.util;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fortify.cli.common.exception.FcliSimpleException;

class AviatorLocalFprHelperTest {
    @TempDir private Path tempDir;

    @Test
    void testMissingFprThrowsException() {
        Path missingFpr = tempDir.resolve("missing.fpr");

        assertThrows(FcliSimpleException.class, () -> AviatorLocalFprHelper.validateLocalFprs(List.of(missingFpr)));
    }

    @Test
    void testDirectoryFprThrowsException() {
        assertThrows(FcliSimpleException.class, () -> AviatorLocalFprHelper.validateLocalFprs(List.of(tempDir)));
    }

    @Test
    void testEmptyFprListThrowsException() {
        assertThrows(FcliSimpleException.class, () -> AviatorLocalFprHelper.validateLocalFprs(List.of()));
    }

    @Test
    void testInvalidFprThrowsException() throws Exception {
        Path invalidFpr = tempDir.resolve("invalid.fpr");
        Files.writeString(invalidFpr, "not a zip");

        assertThrows(FcliSimpleException.class, () -> AviatorLocalFprHelper.validateLocalFprs(List.of(invalidFpr)));
    }
}